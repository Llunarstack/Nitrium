package dev.nitrium.client.culling.terrain;

import dev.nitrium.Nitrium;
import dev.nitrium.client.nativegl.GlContext;
import dev.nitrium.client.nativegl.GlShaderProgram;
import dev.nitrium.client.nativegl.GpuTextureBridge;
import dev.nitrium.config.NitriumConfigManager;
import com.mojang.blaze3d.textures.GpuTexture;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL42;
import org.lwjgl.opengl.GL43;
import org.lwjgl.opengl.GLCapabilities;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * GPU Hi-Z (hierarchical Z-buffer) occlusion culling for chunk sections. Builds a depth pyramid from
 * the main framebuffer each frame and tests AABBs against it on the CPU from readback mips.
 */
public final class HiZOcclusionCuller {
	private static final Identifier DOWNSAMPLE_SHADER = Identifier.fromNamespaceAndPath("nitrium", "shaders/hiz_downsample.comp");
	private static final Identifier DEPTH_COPY_SHADER = Identifier.fromNamespaceAndPath("nitrium", "shaders/hiz_depth_copy.comp");

	private static HiZOcclusionCuller instance;

	private boolean computeSupported;
	private boolean pyramidBuilt;
	private int pyramidWidth;
	private int pyramidHeight;
	private int mipLevels;

	private GlShaderProgram downsampleProgram;
	private GlShaderProgram depthCopyProgram;
	private final List<Integer> pyramidTextures = new ArrayList<>();
	private float[] cpuMip0;

	private HiZOcclusionCuller() {
	}

	public static HiZOcclusionCuller get() {
		if (instance == null) {
			instance = new HiZOcclusionCuller();
		}
		return instance;
	}

	public void probeCapabilities() {
		GLCapabilities caps = GL.getCapabilities();
		computeSupported = caps != null && caps.OpenGL43;
		Nitrium.LOGGER.info("Nitrium Hi-Z: compute shaders {}", computeSupported ? "available" : "unavailable");
	}

	public void buildDepthPyramid(int framebufferWidth, int framebufferHeight) {
		if (!isEnabled() || !computeSupported || !GlContext.isReady()) {
			pyramidBuilt = false;
			return;
		}

		if (framebufferWidth <= 0 || framebufferHeight <= 0) {
			pyramidBuilt = false;
			return;
		}

		if (framebufferWidth != pyramidWidth || framebufferHeight != pyramidHeight) {
			destroyPyramid();
			pyramidWidth = framebufferWidth;
			pyramidHeight = framebufferHeight;
			mipLevels = Math.max(1, 32 - Integer.numberOfLeadingZeros(Math.max(pyramidWidth, pyramidHeight)));
			allocatePyramid();
		}

		if (!ensureShader()) {
			pyramidBuilt = false;
			return;
		}

		copyDepthToMip0();
		buildMipChain();
		readbackMip0();
		pyramidBuilt = cpuMip0 != null;
	}

	public boolean isOccluded(AABB bounds) {
		if (!isEnabled() || !pyramidBuilt || cpuMip0 == null) {
			return false;
		}

		Minecraft client = Minecraft.getInstance();
		Camera camera = client.gameRenderer.getMainCamera();
		float partialTick = client.getDeltaTracker().getGameTimeDeltaPartialTick(false);
		Matrix4f projection = new Matrix4f(client.gameRenderer.getProjectionMatrix(partialTick));

		Matrix4f view = new Matrix4f();
		Quaternionf rotation = camera.rotation();
		view.rotate(rotation.conjugate(new Quaternionf()));
		view.translate(
				-(float) camera.position().x,
				-(float) camera.position().y,
				-(float) camera.position().z
		);
		Matrix4f clip = new Matrix4f(projection).mul(view);

		float minX = Float.POSITIVE_INFINITY;
		float minY = Float.POSITIVE_INFINITY;
		float maxX = Float.NEGATIVE_INFINITY;
		float maxY = Float.NEGATIVE_INFINITY;
		float nearestDepth = 1.0f;

		for (int corner = 0; corner < 8; corner++) {
			double x = (corner & 1) == 0 ? bounds.minX : bounds.maxX;
			double y = (corner & 2) == 0 ? bounds.minY : bounds.maxY;
			double z = (corner & 4) == 0 ? bounds.minZ : bounds.maxZ;

			Vector4f clipPos = new Vector4f((float) x, (float) y, (float) z, 1.0f);
			clipPos.mul(clip);

			if (clipPos.w <= 0.0f) {
				return false;
			}

			float ndcX = clipPos.x / clipPos.w;
			float ndcY = clipPos.y / clipPos.w;
			float ndcZ = clipPos.z / clipPos.w;

			minX = Math.min(minX, ndcX);
			minY = Math.min(minY, ndcY);
			maxX = Math.max(maxX, ndcX);
			maxY = Math.max(maxY, ndcY);
			nearestDepth = Math.min(nearestDepth, ndcZ * 0.5f + 0.5f);
		}

		if (maxX < -1.0f || minX > 1.0f || maxY < -1.0f || minY > 1.0f) {
			return true;
		}

		int x0 = Math.clamp((int) ((minX * 0.5f + 0.5f) * pyramidWidth), 0, pyramidWidth - 1);
		int y0 = Math.clamp((int) ((minY * 0.5f + 0.5f) * pyramidHeight), 0, pyramidHeight - 1);
		int x1 = Math.clamp((int) ((maxX * 0.5f + 0.5f) * pyramidWidth), 0, pyramidWidth - 1);
		int y1 = Math.clamp((int) ((maxY * 0.5f + 0.5f) * pyramidHeight), 0, pyramidHeight - 1);

		float maxOccluderDepth = 0.0f;
		for (int y = y0; y <= y1; y++) {
			for (int x = x0; x <= x1; x++) {
				maxOccluderDepth = Math.max(maxOccluderDepth, sampleMip0(x, y));
			}
		}

		return nearestDepth > maxOccluderDepth + 0.002f;
	}

	private float sampleMip0(int x, int y) {
		return cpuMip0[y * pyramidWidth + x];
	}

	private void readbackMip0() {
		if (pyramidTextures.isEmpty()) {
			return;
		}

		cpuMip0 = new float[pyramidWidth * pyramidHeight];
		FloatBuffer buffer = FloatBuffer.wrap(cpuMip0);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, pyramidTextures.getFirst());
		GL11.glGetTexImage(GL11.GL_TEXTURE_2D, 0, GL11.GL_RED, GL11.GL_FLOAT, buffer);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
	}

	private void copyDepthToMip0() {
		if (!ensureDepthCopyShader()) {
			return;
		}

		Minecraft client = Minecraft.getInstance();
		GpuTexture depthTexture = client.getMainRenderTarget().getDepthTexture();
		int depthGlId = GpuTextureBridge.glId(depthTexture);
		if (depthGlId == 0 || pyramidTextures.isEmpty()) {
			return;
		}

		depthCopyProgram.bind();
		GL13.glActiveTexture(GL13.GL_TEXTURE0);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, depthGlId);
		depthCopyProgram.setUniform1i("uDepth", 0);
		GL42.glBindImageTexture(1, pyramidTextures.getFirst(), 0, false, 0, GL43.GL_WRITE_ONLY, GL30.GL_R32F);

		int groupsX = (pyramidWidth + 7) / 8;
		int groupsY = (pyramidHeight + 7) / 8;
		depthCopyProgram.dispatchCompute(groupsX, groupsY, 1);
		GL42.glMemoryBarrier(GL43.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);
		depthCopyProgram.unbind();

		GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
	}

	private boolean ensureDepthCopyShader() {
		if (depthCopyProgram != null) {
			return true;
		}

		Optional<GlShaderProgram> compiled = GlShaderProgram.compileCompute(
				Minecraft.getInstance().getResourceManager(),
				DEPTH_COPY_SHADER
		);
		depthCopyProgram = compiled.orElse(null);
		return depthCopyProgram != null;
	}

	private void buildMipChain() {
		downsampleProgram.bind();

		int srcWidth = pyramidWidth;
		int srcHeight = pyramidHeight;

		for (int mip = 1; mip < pyramidTextures.size(); mip++) {
			int destWidth = Math.max(1, srcWidth / 2);
			int destHeight = Math.max(1, srcHeight / 2);

			GL42.glBindImageTexture(0, pyramidTextures.get(mip - 1), 0, false, 0, GL43.GL_READ_ONLY, GL30.GL_R32F);
			GL42.glBindImageTexture(1, pyramidTextures.get(mip), 0, false, 0, GL43.GL_WRITE_ONLY, GL30.GL_R32F);

			downsampleProgram.setUniform2f("uSourceSize", srcWidth, srcHeight);

			int groupsX = (destWidth + 7) / 8;
			int groupsY = (destHeight + 7) / 8;
			downsampleProgram.dispatchCompute(groupsX, groupsY, 1);
			GL42.glMemoryBarrier(GL43.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);

			srcWidth = destWidth;
			srcHeight = destHeight;
		}

		downsampleProgram.unbind();
	}

	private boolean ensureShader() {
		if (downsampleProgram != null) {
			return true;
		}

		Optional<GlShaderProgram> compiled = GlShaderProgram.compileCompute(
				Minecraft.getInstance().getResourceManager(),
				DOWNSAMPLE_SHADER
		);
		downsampleProgram = compiled.orElse(null);
		return downsampleProgram != null;
	}

	private void allocatePyramid() {
		for (int mip = 0; mip < mipLevels; mip++) {
			int width = Math.max(1, pyramidWidth >> mip);
			int height = Math.max(1, pyramidHeight >> mip);

			int texture = GL11.glGenTextures();
			GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
			GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_R32F, width, height, 0, GL11.GL_RED, GL11.GL_FLOAT, 0);
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
			GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
			pyramidTextures.add(texture);
		}
	}

	private void destroyPyramid() {
		if (GlContext.isReady()) {
			for (int texture : pyramidTextures) {
				GL11.glDeleteTextures(texture);
			}
		}
		pyramidTextures.clear();
		cpuMip0 = null;
		if (downsampleProgram != null) {
			downsampleProgram.close();
			downsampleProgram = null;
		}
		if (depthCopyProgram != null) {
			depthCopyProgram.close();
			depthCopyProgram = null;
		}
	}

	public boolean isEnabled() {
		return NitriumConfigManager.get().enableHiZOcclusion;
	}

	public boolean isComputeSupported() {
		return computeSupported;
	}

	public boolean isPyramidReady() {
		return pyramidBuilt;
	}

	public void invalidatePyramid() {
		destroyPyramid();
		pyramidBuilt = false;
		pyramidWidth = 0;
		pyramidHeight = 0;
		mipLevels = 0;
	}
}
