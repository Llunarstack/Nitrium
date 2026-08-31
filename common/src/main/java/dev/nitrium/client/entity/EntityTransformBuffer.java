package dev.nitrium.client.entity;

import dev.nitrium.NitriumMod;
import dev.nitrium.client.nativegl.GlContext;
import dev.nitrium.config.NitriumConfigManager;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL43;

/**
 * SSBO holding per-instance position, rotation, and animation frame indices.
 * Layout: 16 floats (mat4) + 1 int animFrame per instance = 68 bytes/instance.
 */
public final class EntityTransformBuffer {
	private static EntityTransformBuffer instance;

	private int ssboId;
	private int capacityInstances;
	private boolean requested;
	private boolean initialized;

	private EntityTransformBuffer() {
	}

	public static EntityTransformBuffer get() {
		if (instance == null) {
			instance = new EntityTransformBuffer();
		}
		return instance;
	}

	/**
	 * Record the requested capacity. The GL buffer itself is created lazily on the render thread
	 * via {@link #ensureGlBuffer()} — never here, since this runs during client init before the
	 * GL context exists.
	 */
	public void init(int maxInstances) {
		if (!NitriumConfigManager.get().enableGpuEntityInstancing) {
			return;
		}
		capacityInstances = maxInstances;
		requested = true;
	}

	private boolean ensureGlBuffer() {
		if (initialized) {
			return true;
		}
		if (!requested || !GlContext.isReady()) {
			return false;
		}

		ssboId = GL15.glGenBuffers();
		// TODO: glBufferData(GL_SHADER_STORAGE_BUFFER, capacity * 68, GL_DYNAMIC_DRAW).
		initialized = true;
		NitriumMod.LOGGER.debug("Nitrium entity transform SSBO: {} instances max", capacityInstances);
		return true;
	}

	public void upload(EntityInstanceBatch batch) {
		if (!ensureGlBuffer()) {
			return;
		}

		// TODO: pack the InstanceData into a native buffer and glBufferSubData it up.
	}

	public int ssboBinding() {
		return 0; // TODO: bind point shared by the compute pass and instanced vertex shader.
	}

	public void bind() {
		if (ensureGlBuffer()) {
			GL43.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, ssboBinding(), ssboId);
		}
	}

	public void close() {
		if (initialized && GlContext.isReady()) {
			GL15.glDeleteBuffers(ssboId);
		}
		initialized = false;
		requested = false;
	}
}
