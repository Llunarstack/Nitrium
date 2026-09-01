package dev.nitrium.client.particles;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.nitrium.Nitrium;
import dev.nitrium.client.mixin.ParticleAccessor;
import dev.nitrium.client.mixin.ParticleEngineAccessor;
import dev.nitrium.client.nativegl.GlContext;
import dev.nitrium.client.nativegl.GlShaderProgram;
import dev.nitrium.config.NitriumConfigManager;
import dev.nitrium.memory.NativeResourceCleaner;
import dev.nitrium.client.culling.CullingPipeline;
import dev.nitrium.client.platform.ClientRenderStages;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.resources.Identifier;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL43;

import java.lang.ref.Cleaner;
import java.nio.ByteBuffer;
import java.util.Optional;

/**
 * GPU compute particle simulation with SSBO storage and instanced point draw.
 */
public final class GpuParticleEngine {
	private static final Identifier UPDATE_SHADER = Identifier.fromNamespaceAndPath("nitrium", "shaders/particle_update.comp");
	private static final Identifier DRAW_VERTEX = Identifier.fromNamespaceAndPath("nitrium", "shaders/particle_draw.vert");
	private static final Identifier DRAW_FRAGMENT = Identifier.fromNamespaceAndPath("nitrium", "shaders/particle_draw.frag");

	private static GpuParticleEngine instance;

	private ParticleSsboPool ssboPool;
	private ParticleIndirectDraw indirectDraw;
	private GlShaderProgram updateProgram;
	private GlShaderProgram drawProgram;
	private int drawVao;
	private final ParticleStats stats = new ParticleStats();
	private final Cleaner.Cleanable cleanable;

	private GpuParticleEngine() {
		this.cleanable = NativeResourceCleaner.register(this, this::destroyGpuResources);
	}

	public static void init() {
		if (instance != null) {
			return;
		}
		instance = new GpuParticleEngine();
		instance.register();
	}

	private void register() {
		int max = NitriumConfigManager.get().maxGpuParticles;
		ssboPool = new ParticleSsboPool(max);
		indirectDraw = new ParticleIndirectDraw();

		ClientRenderStages.onBeforeDebug(this::simulateAndCull);
		ClientRenderStages.onAfterEntities(this::drawParticles);

		Nitrium.LOGGER.info("Nitrium GPU particle engine active (max={})", max);
	}

	private void simulateAndCull() {
		stats.recordSimulatePass();
		if (!GlContext.isReady()) {
			ssboPool.setActiveCount(0);
			return;
		}

		int imported = importVanillaParticles();
		ssboPool.setActiveCount(imported);

		if (imported > 0 && ensureUpdateProgram()) {
			ssboPool.uploadStaging();
			updateProgram.bind();
			updateProgram.setUniform1f("uDeltaTime", 1.0f / 20.0f);
			updateProgram.setUniform1i("uCount", imported);
			int groups = (imported + 255) / 256;
			updateProgram.dispatchCompute(groups, 1, 1);
			GL43.glMemoryBarrier(GL43.GL_SHADER_STORAGE_BARRIER_BIT);
			updateProgram.unbind();
		}
	}

	private int importVanillaParticles() {
		Minecraft client = Minecraft.getInstance();
		if (!(client.particleEngine instanceof ParticleEngineAccessor engineAccessor)) {
			return 0;
		}

		ByteBuffer staging = ssboPool.stagingBuffer();
		staging.clear();

		int count = 0;
		int max = ssboPool.maxParticles();
		for (ParticleGroup<?> group : engineAccessor.nitrium$getParticleGroups().values()) {
			for (Particle particle : group.getAll()) {
				if (count >= max) {
					return count;
				}
				if (particle == null || !particle.isAlive() || !(particle instanceof ParticleAccessor particleAccessor)) {
					continue;
				}

				staging.putFloat((float) particleAccessor.nitrium$getX());
				staging.putFloat((float) particleAccessor.nitrium$getY());
				staging.putFloat((float) particleAccessor.nitrium$getZ());
				staging.putFloat(Math.max(0.05f, particle.getLifetime() / 20.0f));

				staging.putFloat((float) particleAccessor.nitrium$getXd());
				staging.putFloat((float) particleAccessor.nitrium$getYd());
				staging.putFloat((float) particleAccessor.nitrium$getZd());
				staging.putFloat(0.0f);
				count++;
			}
		}

		return count;
	}

	private void drawParticles() {
		int count = ssboPool.activeCount();
		if (count <= 0 || !ensureDrawProgram()) {
			return;
		}

		RenderSystem.assertOnRenderThread();

		GL43.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 0, ssboPool.bufferId());
		drawProgram.bind();
		ensureDrawVao();
		GL30.glBindVertexArray(drawVao);

		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glEnable(0x8642); // GL_PROGRAM_POINT_SIZE
		GL11.glDrawArrays(GL11.GL_POINTS, 0, count);
		GL11.glDisable(0x8642);
		GL11.glDisable(GL11.GL_BLEND);

		GL30.glBindVertexArray(0);
		drawProgram.unbind();
		stats.recordDraw(count);
	}

	private boolean ensureUpdateProgram() {
		if (updateProgram != null) {
			return true;
		}
		Optional<GlShaderProgram> compiled = GlShaderProgram.compileCompute(
				Minecraft.getInstance().getResourceManager(),
				UPDATE_SHADER
		);
		updateProgram = compiled.orElse(null);
		return updateProgram != null;
	}

	private boolean ensureDrawProgram() {
		if (drawProgram != null) {
			return true;
		}
		Optional<GlShaderProgram> compiled = GlShaderProgram.compileGraphics(
				Minecraft.getInstance().getResourceManager(),
				DRAW_VERTEX,
				DRAW_FRAGMENT
		);
		drawProgram = compiled.orElse(null);
		return drawProgram != null;
	}

	private void ensureDrawVao() {
		if (drawVao != 0) {
			return;
		}
		drawVao = GL30.glGenVertexArrays();
		GL30.glBindVertexArray(drawVao);
		GL30.glBindVertexArray(0);
	}

	public ParticleStats stats() {
		return stats;
	}

	public static GpuParticleEngine get() {
		return instance;
	}

	public void onWorldUnload() {
		stats.reset();
		if (ssboPool != null) {
			ssboPool.setActiveCount(0);
		}
	}

	public void shutdown() {
		cleanable.clean();
		instance = null;
	}

	private void destroyGpuResources() {
		if (updateProgram != null) {
			updateProgram.close();
			updateProgram = null;
		}
		if (drawProgram != null) {
			drawProgram.close();
			drawProgram = null;
		}
		if (drawVao != 0 && GlContext.isReady()) {
			GL30.glDeleteVertexArrays(drawVao);
			drawVao = 0;
		}
		if (ssboPool != null) {
			ssboPool.close();
			ssboPool = null;
		}
		if (indirectDraw != null) {
			indirectDraw.close();
			indirectDraw = null;
		}
	}
}
