package dev.nitrium.storage;

import dev.nitrium.Nitrium;
import dev.nitrium.config.NitriumConfigManager;
import dev.nitrium.memory.NitriumWorkerThreads;
import dev.nitrium.nativecore.NativeChunkIo;
import dev.nitrium.platform.ServerEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
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

		ServerEvents.get().serverTickEnd(server -> drainQueue(server));
		ServerEvents.get().serverWorldUnload((server, level) -> {
			if (level.dimension() == level.getServer().overworld().dimension()) {
				ChunkDiskWriter.init(level.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT));
			}
		});

		Nitrium.LOGGER.info("Nitrium async chunk storage active (nativeRing={}, writers={})",
				nativeRing, NitriumConfigManager.get().maxConcurrentChunkWrites);
	}

	public void enqueueChunkSave(ChunkPos pos, byte[] payload) {
		ChunkSavePayload record = ChunkSavePayload.encode(pos, payload);
		byte[] encoded = record.encoded();
		stats.recordEnqueue(encoded.length);

		if (nativeRing && NativeChunkIo.submitWrite(encoded)) {
			stats.recordNativeWrite();
			return;
		}

		if (!fallbackQueue.offer(encoded)) {
			stats.recordDropped();
			Nitrium.LOGGER.warn("Nitrium chunk save queue full — dropping write for {}", pos);
		}
	}

	private void drainQueue(MinecraftServer server) {
		byte[] payload;
		while ((payload = fallbackQueue.poll()) != null) {
			byte[] copy = payload;
			writers.execute(() -> flushPayload(copy));
		}

		if (nativeRing) {
			byte[] nativePayload;
			while ((nativePayload = NativeChunkIo.pollWrite()) != null) {
				byte[] copy = nativePayload;
				writers.execute(() -> flushPayload(copy));
			}
			stats.setNativePending(NativeChunkIo.pendingBytes());
		}
	}

	private static void flushPayload(byte[] encoded) {
		AsyncChunkStorageEngine engine = get();
		if (engine == null) {
			return;
		}

		try {
			ChunkSavePayload payload = ChunkSavePayload.decode(encoded);
			ChunkDiskWriter.writeEncoded(encoded);
			engine.stats.recordFlushed(encoded.length);
		} catch (Exception exception) {
			Nitrium.LOGGER.warn("Failed to flush async chunk payload", exception);
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
