package dev.nitrium.nativecore;

import dev.nitrium.NitriumMod;
import dev.nitrium.config.NitriumConfigManager;

/**
 * Native ring-buffer chunk I/O. Writes are queued into an off-heap ring buffer; the async
 * direct-to-disk drain on the native side is still a stub.
 */
public final class NativeChunkIo {
	private static boolean initialized;

	private NativeChunkIo() {
	}

	public static boolean init() {
		if (initialized) {
			return true;
		}
		if (!NitriumNativeLoader.isAvailable()) {
			NitriumMod.LOGGER.warn("Native chunk I/O unavailable — using Java fallback queue");
			return false;
		}

		long bytes = (long) NitriumConfigManager.get().chunkSaveRingBufferMb * 1024L * 1024L;
		initialized = nativeInitRingBuffer(bytes);
		if (initialized) {
			NitriumMod.LOGGER.info("Nitrium native chunk ring buffer ready ({} MB)", NitriumConfigManager.get().chunkSaveRingBufferMb);
		}
		return initialized;
	}

	public static void shutdown() {
		if (initialized && NitriumNativeLoader.isAvailable()) {
			nativeShutdownRingBuffer();
		}
		initialized = false;
	}

	public static long pendingBytes() {
		if (!initialized || !NitriumNativeLoader.isAvailable()) {
			return 0L;
		}
		return nativePendingBytes();
	}

	public static boolean submitWrite(byte[] payload) {
		if (initialized && NitriumNativeLoader.isAvailable()) {
			return nativeSubmitWrite(payload);
		}
		return false;
	}

	private static native boolean nativeInitRingBuffer(long capacityBytes);

	private static native void nativeShutdownRingBuffer();

	private static native long nativePendingBytes();

	private static native boolean nativeSubmitWrite(byte[] payload);
}
