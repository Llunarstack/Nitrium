package dev.nitrium.worldgen.taskgraph;

import dev.nitrium.Nitrium;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Lock-free work-stealing task graph for off-thread chunk generation, along the lines of C2ME.
 * Uses per-worker deques with atomic steal from the tail of other workers.
 */
public final class LockFreeTaskGraph {
	private final int workerCount;
	private final Deque<ChunkGenTask>[] localQueues;
	private final AtomicLong submitted = new AtomicLong();
	private final AtomicLong completed = new AtomicLong();
	private volatile boolean shutdown;

	@SuppressWarnings("unchecked")
	public LockFreeTaskGraph(int workerCount) {
		this.workerCount = workerCount;
		this.localQueues = new Deque[workerCount];
		for (int i = 0; i < workerCount; i++) {
			localQueues[i] = new ConcurrentLinkedDeque<>();
		}
	}

	public void submit(int workerIndex, ChunkGenTask task) {
		if (shutdown) {
			return;
		}
		localQueues[workerIndex % workerCount].addLast(task);
		submitted.incrementAndGet();
	}

	public ChunkGenTask poll(int workerIndex) {
		Deque<ChunkGenTask> local = localQueues[workerIndex % workerCount];
		ChunkGenTask task = local.pollFirst();
		if (task != null) {
			return task;
		}
		return steal(workerIndex);
	}

	private ChunkGenTask steal(int thiefIndex) {
		for (int victim = 0; victim < workerCount; victim++) {
			if (victim == thiefIndex) {
				continue;
			}
			ChunkGenTask stolen = localQueues[victim].pollLast();
			if (stolen != null) {
				return stolen;
			}
		}
		return null;
	}

	public void markCompleted() {
		completed.incrementAndGet();
	}

	public long submittedCount() {
		return submitted.get();
	}

	public long completedCount() {
		return completed.get();
	}

	public int pendingCount() {
		int pending = 0;
		for (Deque<ChunkGenTask> queue : localQueues) {
			pending += queue.size();
		}
		return pending;
	}

	public void shutdown() {
		shutdown = true;
		for (Deque<ChunkGenTask> queue : localQueues) {
			queue.clear();
		}
		Nitrium.LOGGER.debug("Nitrium worldgen task graph shut down");
	}
}
