package dev.nitrium.worldgen.taskgraph;

import dev.nitrium.NitriumMod;
import dev.nitrium.config.NitriumConfigManager;
import dev.nitrium.memory.NitriumWorkerThreads;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Schedules chunk generation tasks across a lock-free work-stealing graph.
 */
public final class WorldgenTaskScheduler {
	private final LockFreeTaskGraph graph;
	private final ExecutorService workers;
	private final AtomicInteger roundRobin = new AtomicInteger();

	public WorldgenTaskScheduler(int workerCount) {
		this.graph = new LockFreeTaskGraph(workerCount);
		this.workers = NitriumWorkerThreads.createPool(workerCount, "NitriumWorldgen");
	}

	public static WorldgenTaskScheduler create() {
		int threads = NitriumConfigManager.get().worldgenWorkerThreads;
		return new WorldgenTaskScheduler(Math.max(1, threads));
	}

	public void submit(ChunkGenTask task) {
		int worker = Math.floorMod(roundRobin.getAndIncrement(), graph.pendingCount() + 1);
		graph.submit(worker, task);
		workers.execute(() -> executeTask(task));
	}

	private void executeTask(ChunkGenTask task) {
		if (!task.isReady()) {
			return;
		}
		try {
			task.run();
		} catch (Exception exception) {
			NitriumMod.LOGGER.warn("Worldgen task {} failed for chunk {}", task.type(), task.chunkPos(), exception);
		} finally {
			graph.markCompleted();
		}
	}

	public LockFreeTaskGraph graph() {
		return graph;
	}

	public void shutdown() {
		graph.shutdown();
		workers.shutdown();
		try {
			if (!workers.awaitTermination(5, TimeUnit.SECONDS)) {
				workers.shutdownNow();
			}
		} catch (InterruptedException exception) {
			workers.shutdownNow();
			Thread.currentThread().interrupt();
		}
	}
}
