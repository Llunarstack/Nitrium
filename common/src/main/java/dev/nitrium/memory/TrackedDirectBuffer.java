package dev.nitrium.memory;

import java.lang.ref.Cleaner;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Tracks a direct {@link ByteBuffer} with {@link NativeResourceCleaner} fallback.
 */
public final class TrackedDirectBuffer implements AutoCloseable {
	private final ByteBuffer buffer;
	private final Cleaner.Cleanable cleanable;
	private boolean closed;

	private TrackedDirectBuffer(ByteBuffer buffer, Runnable onClose) {
		this.buffer = buffer;
		this.cleanable = NativeResourceCleaner.register(this, onClose);
	}

	public static TrackedDirectBuffer allocate(int bytes) {
		ByteBuffer buffer = ByteBuffer.allocateDirect(bytes).order(ByteOrder.nativeOrder());
		return new TrackedDirectBuffer(buffer, () -> {
			// Direct buffers are freed by GC; Cleaner is a diagnostic safety net.
		});
	}

	public ByteBuffer buffer() {
		return buffer;
	}

	@Override
	public void close() {
		if (closed) {
			return;
		}
		closed = true;
		cleanable.clean();
	}
}
