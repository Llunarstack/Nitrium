package dev.nitrium.client.entity;

import dev.nitrium.Nitrium;
import dev.nitrium.client.nativegl.GlContext;
import dev.nitrium.config.NitriumConfigManager;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL43;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * SSBO holding per-instance position, rotation, and animation frame indices.
 * Layout: 16 floats (mat4) + 1 int animFrame per instance = 68 bytes/instance.
 */
public final class EntityTransformBuffer {
	public static final int BINDING_POINT = 1;
	public static final int BYTES_PER_INSTANCE = 68;

	private static EntityTransformBuffer instance;

	private int ssboId;
	private int capacityInstances;
	private boolean requested;
	private boolean initialized;
	private ByteBuffer uploadScratch;

	private EntityTransformBuffer() {
	}

	public static EntityTransformBuffer get() {
		if (instance == null) {
			instance = new EntityTransformBuffer();
		}
		return instance;
	}

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
		long bytes = (long) capacityInstances * BYTES_PER_INSTANCE;
		GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, ssboId);
		GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, bytes, GL15.GL_DYNAMIC_DRAW);
		GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);

		uploadScratch = MemoryUtil.memAlloc((int) Math.min(bytes, Integer.MAX_VALUE));
		initialized = true;
		Nitrium.LOGGER.debug("Nitrium entity transform SSBO: {} instances max", capacityInstances);
		return true;
	}

	public void upload(EntityInstanceBatch batch) {
		if (!ensureGlBuffer()) {
			return;
		}

		int count = 0;
		uploadScratch.clear();

		for (List<EntityInstanceBatch.InstanceData> instances : batch.batches().values()) {
			for (EntityInstanceBatch.InstanceData data : instances) {
				if (count >= capacityInstances) {
					break;
				}

				Matrix4f matrix = data.transform();
				uploadScratch.putFloat(matrix.m00()).putFloat(matrix.m10()).putFloat(matrix.m20()).putFloat(matrix.m30());
				uploadScratch.putFloat(matrix.m01()).putFloat(matrix.m11()).putFloat(matrix.m21()).putFloat(matrix.m31());
				uploadScratch.putFloat(matrix.m02()).putFloat(matrix.m12()).putFloat(matrix.m22()).putFloat(matrix.m32());
				uploadScratch.putFloat(matrix.m03()).putFloat(matrix.m13()).putFloat(matrix.m23()).putFloat(matrix.m33());
				uploadScratch.putInt(data.animationFrame());
				count++;
			}
		}

		if (count == 0) {
			return;
		}

		uploadScratch.flip();
		GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, ssboId);
		GL15.glBufferSubData(GL43.GL_SHADER_STORAGE_BUFFER, 0, uploadScratch);
		GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
	}

	public int ssboBinding() {
		return BINDING_POINT;
	}

	public void bind() {
		if (ensureGlBuffer()) {
			GL43.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, ssboBinding(), ssboId);
		}
	}

	public void close() {
		if (uploadScratch != null) {
			MemoryUtil.memFree(uploadScratch);
			uploadScratch = null;
		}
		if (initialized && GlContext.isReady()) {
			GL15.glDeleteBuffers(ssboId);
		}
		initialized = false;
		requested = false;
	}
}
