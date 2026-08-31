package dev.nitrium.client.streaming;

import dev.nitrium.config.NitriumConfigManager;

import java.util.Comparator;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Off-thread mesh build queue, ordered by {@link MeshTaskPriority}. Draining currently just hands
 * tasks back to the caller; feeding the results into Sodium-compatible GPU buffers is still to do.
 */
public final class MeshTaskScheduler {
	private final PriorityBlockingQueue<MeshTask> queue = new PriorityBlockingQueue<>(128, Comparator.comparingDouble(MeshTask::priority).reversed());
	private final AtomicLong taskCounter = new AtomicLong();

	public long enqueue(SectionKey key, CompactSectionData data, double priority) {
		long id = taskCounter.incrementAndGet();
		queue.offer(new MeshTask(id, key, data, priority));
		return id;
	}

	public int pendingCount() {
		return queue.size();
	}

	public void drainUpTo(int maxTasks, Consumer<MeshTask> consumer) {
		int limit = Math.min(maxTasks, NitriumConfigManager.get().maxConcurrentMeshTasks);
		for (int i = 0; i < limit; i++) {
			MeshTask task = queue.poll();
			if (task == null) {
				break;
			}
			consumer.accept(task);
		}
	}

	public record MeshTask(long id, SectionKey key, CompactSectionData data, double priority) {
	}
}
