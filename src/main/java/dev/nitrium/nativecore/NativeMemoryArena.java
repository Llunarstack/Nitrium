package dev.nitrium.nativecore;

import java.lang.ref.Cleaner;
import dev.nitrium.memory.NativeResourceCleaner;
import dev.nitrium.memory.TrackedDirectBuffer;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Hybrid off-heap allocator with 128 MB page pooling and Cleaner fallbacks.
 */
public final class NativeMemoryArena implements AutoCloseable {
	private static final int PAGE_BYTES = 128 * 1024 * 1024;

	private final Deque<ByteBuffer> pool = new ArrayDeque<>();
	private final Deque<TrackedDirectBuffer> pages = new ArrayDeque<>();
	private final boolean nativeBacked;
	private long pooledBytes;
	private final Cleaner.Cleanable cleanable;

	private NativeMemoryArena(boolean nativeBacked) {
		this.nativeBacked = nativeBacked;
		this.cleanable = NativeResourceCleaner.register(this, this::releaseAll);
	}

	public static NativeMemoryArena create() {
		boolean nativeBacked = NitriumNativeLoader.isAvailable();
		return new NativeMemoryArena(nativeBacked);
	}

	public synchronized ByteBuffer allocate(int bytes) {
		for (ByteBuffer buffer : pool) {
			if (buffer.capacity() >= bytes) {
				pool.remove(buffer);
				buffer.clear();
				return buffer;
			}
		}

		if (bytes <= PAGE_BYTES) {
			TrackedDirectBuffer page = TrackedDirectBuffer.allocate(PAGE_BYTES);
			pages.offer(page);
			ByteBuffer buffer = page.buffer();
			pooledBytes += PAGE_BYTES;
			return buffer;
		}

		TrackedDirectBuffer large = TrackedDirectBuffer.allocate(bytes);
		pages.offer(large);
		pooledBytes += bytes;
		return large.buffer();
	}

	public synchronized void release(ByteBuffer buffer) {
		pool.offer(buffer);
	}

	public long nativeAllocate(long bytes) {
		if (!nativeBacked) {
			return 0L;
		}
		return nativeAlloc(bytes);
	}

	public void resetNativeArena() {
		if (nativeBacked) {
			nativeReset();
		}
	}

	public long pooledBytes() {
		return pooledBytes;
	}

	@Override
	public void close() {
		cleanable.clean();
	}

	private synchronized void releaseAll() {
		pool.clear();
		while (!pages.isEmpty()) {
			pages.poll().close();
		}
		pooledBytes = 0;
		if (nativeBacked) {
			nativeShutdown();
		}
	}

	private static native long nativeAlloc(long bytes);

	private static native void nativeReset();

	private static native void nativeShutdown();
}
