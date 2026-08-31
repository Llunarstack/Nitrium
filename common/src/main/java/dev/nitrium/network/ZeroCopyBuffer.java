package dev.nitrium.network;

import dev.nitrium.memory.TrackedDirectBuffer;

import java.nio.ByteBuffer;

/**
 * Zero-copy off-heap buffer for Netty packet I/O without intermediate heap byte[].
 */
public final class ZeroCopyBuffer implements AutoCloseable {
	private final TrackedDirectBuffer backing;
	private final ByteBuffer buffer;

	public ZeroCopyBuffer(int capacity) {
		this.backing = TrackedDirectBuffer.allocate(capacity);
		this.buffer = backing.buffer();
	}

	public ByteBuffer buffer() {
		return buffer;
	}

	public int capacity() {
		return buffer.capacity();
	}

	@Override
	public void close() {
		backing.close();
	}
}
