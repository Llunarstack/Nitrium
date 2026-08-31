package dev.nitrium.client.culling.terrain;

import dev.nitrium.Nitrium;
import dev.nitrium.config.NitriumConfigManager;
import net.minecraft.world.phys.AABB;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;

/**
 * GPU Hi-Z (hierarchical Z-buffer) occlusion culling for chunk sections. The compute-shader depth
 * pyramid isn't built yet, so for now this just probes capabilities and reports nothing occluded
 * (fail-open) — rendering stays correct while the rest of the pipeline is wired up.
 */
public final class HiZOcclusionCuller {
	private static HiZOcclusionCuller instance;

	private boolean computeSupported;
	private boolean pyramidBuilt;
	private int pyramidWidth;
	private int pyramidHeight;

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

	/**
	 * Downsample the previous frame's depth buffer into a mipmap pyramid.
	 * Called at end of world render, before next frame's culling pass.
	 */
	public void buildDepthPyramid(int framebufferWidth, int framebufferHeight) {
		if (!isEnabled() || !computeSupported) {
			pyramidBuilt = false;
			return;
		}

		pyramidWidth = framebufferWidth;
		pyramidHeight = framebufferHeight;
		// TODO: dispatch the compute shader to build the Hi-Z texture pyramid.
		pyramidBuilt = false;
	}

	/**
	 * @return {@code true} if the bounding box is fully occluded by the Hi-Z pyramid.
	 */
	public boolean isOccluded(AABB bounds) {
		if (!isEnabled() || !pyramidBuilt) {
			return false;
		}

		// TODO: project the AABB corners to screen space and sample the Hi-Z mip chain.
		return false;
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
		pyramidBuilt = false;
		pyramidWidth = 0;
		pyramidHeight = 0;
	}
}
