package dev.nitrium.lighting;

import dev.nitrium.Nitrium;
import dev.nitrium.config.NitriumConfigManager;
import dev.nitrium.memory.NitriumWorkerThreads;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Off-thread worker pool executing light propagation DAG nodes.
 */
public final class AsyncLightWorkerPool {
	private final ExecutorService workers;
	private final AtomicLong tasksSubmitted = new AtomicLong();
	private final AtomicLong tasksCompleted = new AtomicLong();

	public AsyncLightWorkerPool(int threadCount) {
		this.workers = NitriumWorkerThreads.createPool(threadCount, "NitriumLight");
	}

	public void submit(LightPropagationDag dag) {
		tasksSubmitted.incrementAndGet();
		workers.execute(() -> {
			try {
				dag.executeReady();
			} finally {
				tasksCompleted.incrementAndGet();
			}
		});
	}

	public void submit(Runnable task) {
		tasksSubmitted.incrementAndGet();
		workers.execute(() -> {
			try {
				task.run();
			} finally {
				tasksCompleted.incrementAndGet();
			}
		});
	}

	public long submittedCount() {
		return tasksSubmitted.get();
	}

	public long completedCount() {
		return tasksCompleted.get();
	}

	public void shutdown() {
		workers.shutdown();
		try {
			if (!workers.awaitTermination(5, TimeUnit.SECONDS)) {
				workers.shutdownNow();
			}
		} catch (InterruptedException exception) {
			workers.shutdownNow();
			Thread.currentThread().interrupt();
		}
		Nitrium.LOGGER.debug("Nitrium light worker pool shut down");
	}

	public static AsyncLightWorkerPool create() {
		int threads = Math.max(1, NitriumConfigManager.get().lightWorkerThreads);
		return new AsyncLightWorkerPool(threads);
	}
}
