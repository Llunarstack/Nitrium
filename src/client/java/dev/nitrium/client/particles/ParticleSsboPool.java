package dev.nitrium.client.particles;

import dev.nitrium.client.nativegl.GlContext;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL43;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

/**
 * GPU Shader Storage Buffer for active particle state (position, velocity, life, type).
 *
 * <p>The GL buffer is allocated lazily on first use ({@link GlContext}); only the CPU-side staging
 * buffer is allocated in the constructor, so this is safe to instantiate during client init.
 */
public final class ParticleSsboPool implements AutoCloseable {
	public static final int BYTES_PER_PARTICLE = 32;

	private int bufferId;
	private final int maxParticles;
	private final ByteBuffer mappedView;
	private int activeCount;

	public ParticleSsboPool(int maxParticles) {
		this.maxParticles = maxParticles;
		long bytes = (long) maxParticles * BYTES_PER_PARTICLE;
		this.mappedView = MemoryUtil.memAlloc((int) Math.min(bytes, Integer.MAX_VALUE));
	}

	private boolean ensureBuffer() {
		if (bufferId != 0) {
			return true;
		}
		if (!GlContext.isReady()) {
			return false;
		}
		long bytes = (long) maxParticles * BYTES_PER_PARTICLE;
		bufferId = GL15.glGenBuffers();
		GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, bufferId);
		GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, bytes, GL15.GL_DYNAMIC_DRAW);
		GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
		return true;
	}

	public int bufferId() {
		ensureBuffer();
		return bufferId;
	}

	public int maxParticles() {
		return maxParticles;
	}

	public int activeCount() {
		return activeCount;
	}

	public void setActiveCount(int count) {
		activeCount = Math.min(count, maxParticles);
	}

	public ByteBuffer stagingBuffer() {
		return mappedView;
	}

	public void uploadStaging() {
		if (!ensureBuffer()) {
			return;
		}
		mappedView.flip();
		GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, bufferId);
		GL15.glBufferSubData(GL43.GL_SHADER_STORAGE_BUFFER, 0, mappedView);
		GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
		mappedView.clear();
	}

	@Override
	public void close() {
		if (bufferId != 0 && GlContext.isReady()) {
			GL15.glDeleteBuffers(bufferId);
		}
		bufferId = 0;
		MemoryUtil.memFree(mappedView);
	}
}
