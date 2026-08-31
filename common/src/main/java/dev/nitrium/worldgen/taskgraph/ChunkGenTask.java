package dev.nitrium.worldgen.taskgraph;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Single chunk generation unit in the lock-free task graph.
 * Holds only primitive chunk coordinates — no ServerLevel references during initial passes.
 */
public final class ChunkGenTask implements Runnable {
	private static final AtomicInteger ID_GENERATOR = new AtomicInteger();

	private final int id = ID_GENERATOR.incrementAndGet();
	private final long chunkPos;
	private final ChunkGenTaskType type;
	private final Runnable work;
	private final AtomicInteger dependencies = new AtomicInteger(0);

	public ChunkGenTask(long chunkPos, ChunkGenTaskType type, Runnable work) {
		this.chunkPos = chunkPos;
		this.type = type;
		this.work = work;
	}

	public int id() {
		return id;
	}

	public long chunkPos() {
		return chunkPos;
	}

	public ChunkGenTaskType type() {
		return type;
	}

	public int dependencies() {
		return dependencies.get();
	}

	public void addDependency() {
		dependencies.incrementAndGet();
	}

	public void dependencyCompleted() {
		dependencies.decrementAndGet();
	}

	public boolean isReady() {
		return dependencies.get() <= 0;
	}

	@Override
	public void run() {
		work.run();
	}
}
