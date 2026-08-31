package dev.nitrium.client.culling.entity;

import dev.nitrium.NitriumMod;
import org.lwjgl.opengl.GL33;

/**
 * GPU occlusion queries for entity proxy boxes: render the proxies into an
 * {@code entityProxyBufferSize}² offscreen FBO, then read back {@link GL33#GL_SAMPLES_PASSED} per
 * entity. Not implemented yet — every method here is a stub, so {@link #isOccluded} always says
 * visible.
 */
public final class GpuEntityOcclusionQuery {
	private static GpuEntityOcclusionQuery instance;
	private boolean initialized;

	private GpuEntityOcclusionQuery() {
	}

	public static GpuEntityOcclusionQuery get() {
		if (instance == null) {
			instance = new GpuEntityOcclusionQuery();
		}
		return instance;
	}

	public void init(int bufferSize) {
		if (initialized) {
			return;
		}

		// TODO: allocate the low-res FBO + depth attachment at bufferSize x bufferSize.
		initialized = true;
		NitriumMod.LOGGER.debug("Nitrium entity occlusion proxy buffer: {}x{}", bufferSize, bufferSize);
	}

	/**
	 * @return true if the proxy bounding box has zero visible samples (fully occluded).
	 */
	public boolean isOccluded(int queryId) {
		if (!initialized) {
			return false;
		}

		// TODO: read the sample count back with glGetQueryObjectuiv and return count == 0.
		return false;
	}

	public void beginFrame() {
		// TODO: bind the proxy FBO and clear depth.
	}

	public void endFrame() {
		// TODO: unbind the proxy FBO.
	}
}
