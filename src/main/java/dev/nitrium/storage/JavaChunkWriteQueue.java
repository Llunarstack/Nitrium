package dev.nitrium.storage;

import dev.nitrium.nativecore.NativeChunkIo;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Java fallback queue when native ring buffer is unavailable.
 */
public final class JavaChunkWriteQueue {
	private final BlockingQueue<byte[]> queue;

	public JavaChunkWriteQueue(int capacity) {
		this.queue = new ArrayBlockingQueue<>(capacity);
	}

	public boolean offer(byte[] payload) {
		return queue.offer(payload);
	}

	public byte[] poll() {
		return queue.poll();
	}

	public int size() {
		return queue.size();
	}

	public void clear() {
		queue.clear();
	}
}
