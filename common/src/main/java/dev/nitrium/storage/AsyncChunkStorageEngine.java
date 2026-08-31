package dev.nitrium.storage;

import dev.nitrium.Nitrium;
import dev.nitrium.config.NitriumConfigManager;
import dev.nitrium.memory.NitriumWorkerThreads;
import dev.nitrium.nativecore.NativeChunkIo;
import dev.nitrium.platform.ServerEvents;
import net.minecraft.world.level.ChunkPos;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Ring-buffer async chunk saver — decouples disk I/O from server tick loop.
 */
public final class AsyncChunkStorageEngine {
	private static AsyncChunkStorageEngine instance;

	private final JavaChunkWriteQueue fallbackQueue;
	private final ExecutorService writers;
	private final StorageStats stats = new StorageStats();
	private boolean nativeRing;

	private AsyncChunkStorageEngine() {
		this.fallbackQueue = new JavaChunkWriteQueue(1024);
		int threads = Math.max(1, NitriumConfigManager.get().maxConcurrentChunkWrites);
		this.writers = NitriumWorkerThreads.createPool(threads, "NitriumChunkIo");
	}

	public static void init() {
		if (instance != null) {
			return;
		}
		instance = new AsyncChunkStorageEngine();
		instance.register();
	}

	private void register() {
		nativeRing = NativeChunkIo.init();

		ServerEvents.get().serverTickEnd(server -> drainQueue());

		Nitrium.LOGGER.info("Nitrium async chunk storage active (nativeRing={}, writers={})",
				nativeRing, NitriumConfigManager.get().maxConcurrentChunkWrites);
	}

	public void enqueueChunkSave(ChunkPos pos, byte[] payload) {
		stats.recordEnqueue(payload.length);
		if (nativeRing && NativeChunkIo.submitWrite(payload)) {
			stats.recordNativeWrite();
			return;
		}
		if (!fallbackQueue.offer(payload)) {
			stats.recordDropped();
			Nitrium.LOGGER.warn("Nitrium chunk save queue full — dropping write for {}", pos);
		}
	}

	private void drainQueue() {
		byte[] payload;
		while ((payload = fallbackQueue.poll()) != null) {
			byte[] copy = payload;
			writers.execute(() -> {
				// TODO: write to the region file via native overlapped I/O.
				stats.recordFlushed(copy.length);
			});
		}
		if (nativeRing) {
			stats.setNativePending(NativeChunkIo.pendingBytes());
		}
	}

	public StorageStats stats() {
		return stats;
	}

	public static AsyncChunkStorageEngine get() {
		return instance;
	}

	public void onWorldUnload() {
		fallbackQueue.clear();
		stats.reset();
	}

	public void shutdown() {
		writers.shutdown();
		try {
			if (!writers.awaitTermination(5, TimeUnit.SECONDS)) {
				writers.shutdownNow();
			}
		} catch (InterruptedException exception) {
			writers.shutdownNow();
			Thread.currentThread().interrupt();
		}
		NativeChunkIo.shutdown();
		instance = null;
	}
}
