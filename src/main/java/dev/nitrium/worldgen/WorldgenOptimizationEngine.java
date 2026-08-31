package dev.nitrium.worldgen;

import dev.nitrium.NitriumMod;
import dev.nitrium.config.NitriumConfigManager;
import dev.nitrium.worldgen.noise.CoarseDensityPipeline;
import dev.nitrium.worldgen.taskgraph.ChunkGenTask;
import dev.nitrium.worldgen.taskgraph.ChunkGenTaskType;
import dev.nitrium.worldgen.taskgraph.WorldgenTaskScheduler;
import dev.nitrium.platform.ServerEvents;
import net.minecraft.world.level.ChunkPos;

/**
 * Server-side worldgen optimization engine: SIMD noise, coarse-grid density, lock-free task graph.
 */
public final class WorldgenOptimizationEngine {
	private static WorldgenOptimizationEngine instance;

	private final CoarseDensityPipeline densityPipeline = CoarseDensityPipeline.create();
	private final WorldgenTaskScheduler taskScheduler;
	private final WorldgenStats stats = new WorldgenStats();
	private long lastStatsLogTick;

	private WorldgenOptimizationEngine() {
		this.taskScheduler = WorldgenTaskScheduler.create();
	}

	public static void init() {
		if (instance != null) {
			return;
		}

		if (!NitriumConfigManager.get().enableWorldgenOptimization) {
			NitriumMod.LOGGER.info("Nitrium worldgen optimization disabled via config");
			return;
		}

		instance = new WorldgenOptimizationEngine();
		instance.register();
	}

	private void register() {
		ServerEvents.get().serverTickEnd(server -> {
			if (server.getTickCount() - lastStatsLogTick >= 6000) {
				lastStatsLogTick = server.getTickCount();
				logStats();
			}
		});

		NitriumMod.LOGGER.info(
				"Nitrium worldgen engine active (simd={}, workers={})",
				NitriumConfigManager.get().enableSimdNoise,
				NitriumConfigManager.get().worldgenWorkerThreads
		);
	}

	/**
	 * Called from the mixin whenever vanilla schedules chunk generation.
	 */
	public void onChunkGenerationScheduled(long chunkPos) {
		stats.recordTaskSubmitted();
		ChunkGenTask task = new ChunkGenTask(chunkPos, ChunkGenTaskType.DENSITY_SPLINE, () -> {
			runDensityPass(chunkPos);
			stats.recordTaskCompleted();
		});
		taskScheduler.submit(task);
	}

	/**
	 * Runs the density pipeline for a chunk section to exercise it. Not yet wired into NoiseChunk,
	 * so this measures the pass rather than feeding real generation.
	 */
	public void runDensityPass(long chunkPos) {
		ChunkPos pos = new ChunkPos(chunkPos);
		long start = System.nanoTime();

		var result = densityPipeline.sampleChunkSection(
				pos.getMinBlockX(),
				-64,
				pos.getMinBlockZ(),
				16,
				384,
				16,
				(int) (chunkPos ^ (chunkPos >>> 32))
		);

		long elapsed = System.nanoTime() - start;
		stats.recordChunkGeneration(elapsed, result.coarseSampleCount(), result.refinedVoxelCount());
	}

	public WorldgenStats stats() {
		return stats;
	}

	public WorldgenTaskScheduler taskScheduler() {
		return taskScheduler;
	}

	public static WorldgenOptimizationEngine get() {
		return instance;
	}

	public void onWorldUnload() {
		stats.reset();
	}

	public void shutdown() {
		taskScheduler.shutdown();
		instance = null;
	}

	private void logStats() {
		if (stats.chunksProcessed() == 0) {
			return;
		}
		NitriumMod.LOGGER.debug(
				"Nitrium worldgen: {} chunks, avg {}ms, coarse={}, refined={}, tasks={}/{}",
				stats.chunksProcessed(),
				String.format("%.2f", stats.averageGenerationMillis()),
				stats.totalCoarseSamples(),
				stats.totalRefinedVoxels(),
				stats.tasksCompleted(),
				stats.tasksSubmitted()
		);
	}
}
