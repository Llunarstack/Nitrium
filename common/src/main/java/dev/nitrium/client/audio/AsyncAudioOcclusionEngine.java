package dev.nitrium.client.audio;

import dev.nitrium.NitriumMod;
import dev.nitrium.config.NitriumConfigManager;
import dev.nitrium.memory.NitriumWorkerThreads;
import dev.nitrium.client.platform.ClientEvents;
import net.minecraft.core.BlockPos;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

/**
 * Off-thread audio occlusion using a coarse voxel grid instead of main-thread raycasts.
 */
public final class AsyncAudioOcclusionEngine {
	private static AsyncAudioOcclusionEngine instance;

	private final AudioVoxelGrid voxelGrid;
	private final ExecutorService workers;
	private final Map<Long, AudioOcclusionResult> cache = new ConcurrentHashMap<>();
	private final AudioStats stats = new AudioStats();

	private AsyncAudioOcclusionEngine() {
		int voxelSize = NitriumConfigManager.get().audioVoxelSizeBlocks;
		this.voxelGrid = new AudioVoxelGrid(voxelSize);
		int threads = Math.max(1, NitriumConfigManager.get().audioWorkerThreads);
		this.workers = NitriumWorkerThreads.createPool(threads, "NitriumAudio");
	}

	public static void init() {
		if (instance != null) {
			return;
		}
		instance = new AsyncAudioOcclusionEngine();
		instance.register();
	}

	private void register() {
		ClientEvents.get().clientTickEnd(client -> cache.clear());

		NitriumMod.LOGGER.info("Nitrium async audio occlusion active (voxel={} blocks, workers={})",
				NitriumConfigManager.get().audioVoxelSizeBlocks,
				NitriumConfigManager.get().audioWorkerThreads);
	}

	public CompletableFuture<AudioOcclusionResult> queryAsync(BlockPos listener, BlockPos source) {
		long key = pack(listener, source);
		AudioOcclusionResult cached = cache.get(key);
		if (cached != null) {
			stats.recordCacheHit();
			return CompletableFuture.completedFuture(cached);
		}

		stats.recordQuerySubmitted();
		return CompletableFuture.supplyAsync(() -> {
			boolean occluded = voxelGrid.isOccluded(
					listener.getX(), listener.getY(), listener.getZ(),
					source.getX(), source.getY(), source.getZ()
			);
			AudioOcclusionResult result = occluded ? AudioOcclusionResult.blocked(0.35f) : AudioOcclusionResult.clear();
			cache.put(key, result);
			stats.recordQueryCompleted();
			return result;
		}, workers);
	}

	public AudioVoxelGrid voxelGrid() {
		return voxelGrid;
	}

	public AudioStats stats() {
		return stats;
	}

	public static AsyncAudioOcclusionEngine get() {
		return instance;
	}

	public void onWorldUnload() {
		cache.clear();
		stats.reset();
	}

	public void shutdown() {
		workers.shutdownNow();
		instance = null;
	}

	private static long pack(BlockPos a, BlockPos b) {
		return ((long) a.asLong() << 32) ^ b.asLong();
	}
}
