package dev.nitrium.storage;

/**
 * Async chunk storage profiling counters.
 */
public final class StorageStats {
	private long enqueued;
	private long flushedBytes;
	private long nativeWrites;
	private long dropped;
	private long nativePending;

	public void recordEnqueue(int bytes) {
		enqueued++;
	}

	public void recordFlushed(int bytes) {
		flushedBytes += bytes;
	}

	public void recordNativeWrite() {
		nativeWrites++;
	}

	public void recordDropped() {
		dropped++;
	}

	public void setNativePending(long bytes) {
		nativePending = bytes;
	}

	public void reset() {
		enqueued = 0;
		flushedBytes = 0;
		nativeWrites = 0;
		dropped = 0;
		nativePending = 0;
	}

	public long enqueued() {
		return enqueued;
	}

	public long flushedBytes() {
		return flushedBytes;
	}

	public long nativeWrites() {
		return nativeWrites;
	}

	public long dropped() {
		return dropped;
	}

	public long nativePending() {
		return nativePending;
	}
}
