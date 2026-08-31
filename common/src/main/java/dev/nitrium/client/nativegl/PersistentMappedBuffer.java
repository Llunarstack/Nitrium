package dev.nitrium.client.nativegl;

import dev.nitrium.memory.NativeResourceCleaner;
import dev.nitrium.Nitrium;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL44;

import java.nio.ByteBuffer;
import java.lang.ref.Cleaner;

/**
 * OpenGL 4.4+ AZDO persistent mapped buffer (GL_ARB_buffer_storage).
 * Streams chunk/entity geometry to GPU without per-frame {@code glBufferSubData}.
 */
public final class PersistentMappedBuffer implements AutoCloseable {
	private final int bufferId;
	private final long capacity;
	private final ByteBuffer mapped;
	private final boolean persistent;
	private final Cleaner.Cleanable cleanable;
	private boolean closed;

	public PersistentMappedBuffer(long capacityBytes) {
		this.capacity = capacityBytes;
		this.bufferId = GL15.glGenBuffers();

		int flags = GL44.GL_MAP_WRITE_BIT
				| GL44.GL_MAP_PERSISTENT_BIT
				| GL44.GL_MAP_COHERENT_BIT;

		GL15.glBindBuffer(GL30.GL_ARRAY_BUFFER, bufferId);
		GL44.glBufferStorage(GL30.GL_ARRAY_BUFFER, capacityBytes, flags);

		ByteBuffer mapping = GL30.glMapBufferRange(
				GL30.GL_ARRAY_BUFFER,
				0,
				capacityBytes,
				flags
		);

		if (mapping == null) {
			persistent = false;
			mapped = null;
			Nitrium.LOGGER.warn("Persistent buffer mapping unavailable — falling back to dynamic buffer");
			GL15.glBufferData(GL30.GL_ARRAY_BUFFER, capacityBytes, GL15.GL_DYNAMIC_DRAW);
		} else {
			persistent = true;
			mapped = mapping;
			Nitrium.LOGGER.info("Persistent mapped buffer allocated: {} bytes", capacityBytes);
		}

		GL15.glBindBuffer(GL30.GL_ARRAY_BUFFER, 0);
		this.cleanable = NativeResourceCleaner.register(this, this::destroyGlBuffer);
	}

	public static boolean isSupported() {
		return GL.getCapabilities() != null && GL.getCapabilities().GL_ARB_buffer_storage;
	}

	public int bufferId() {
		return bufferId;
	}

	public ByteBuffer map() {
		return mapped;
	}

	public boolean isPersistent() {
		return persistent;
	}

	public void write(long offset, ByteBuffer data) {
		if (persistent && mapped != null) {
			mapped.position((int) offset);
			mapped.put(data);
		} else {
			GL15.glBindBuffer(GL30.GL_ARRAY_BUFFER, bufferId);
			GL15.glBufferSubData(GL30.GL_ARRAY_BUFFER, offset, data);
			GL15.glBindBuffer(GL30.GL_ARRAY_BUFFER, 0);
		}
	}

	@Override
	public void close() {
		if (closed) {
			return;
		}
		closed = true;
		cleanable.clean();
	}

	private void destroyGlBuffer() {
		if (persistent && mapped != null) {
			GL15.glBindBuffer(GL30.GL_ARRAY_BUFFER, bufferId);
			GL30.glUnmapBuffer(GL30.GL_ARRAY_BUFFER);
			GL15.glBindBuffer(GL30.GL_ARRAY_BUFFER, 0);
		}
		GL15.glDeleteBuffers(bufferId);
	}
}
