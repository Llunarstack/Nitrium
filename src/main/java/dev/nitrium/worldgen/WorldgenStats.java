package dev.nitrium.worldgen;

/**
 * Rolling statistics for worldgen optimization profiling.
 */
public final class WorldgenStats {
	private long chunksProcessed;
	private long totalGenerationNanos;
	private long totalCoarseSamples;
	private long totalRefinedVoxels;
	private long tasksSubmitted;
	private long tasksCompleted;

	public void recordChunkGeneration(long nanos, int coarseSamples, int refinedVoxels) {
		chunksProcessed++;
		totalGenerationNanos += nanos;
		totalCoarseSamples += coarseSamples;
		totalRefinedVoxels += refinedVoxels;
	}

	public void recordTaskSubmitted() {
		tasksSubmitted++;
	}

	public void recordTaskCompleted() {
		tasksCompleted++;
	}

	public void reset() {
		chunksProcessed = 0;
		totalGenerationNanos = 0;
		totalCoarseSamples = 0;
		totalRefinedVoxels = 0;
		tasksSubmitted = 0;
		tasksCompleted = 0;
	}

	public long chunksProcessed() {
		return chunksProcessed;
	}

	public double averageGenerationMillis() {
		return chunksProcessed == 0 ? 0.0 : (totalGenerationNanos / 1_000_000.0) / chunksProcessed;
	}

	public long totalCoarseSamples() {
		return totalCoarseSamples;
	}

	public long totalRefinedVoxels() {
		return totalRefinedVoxels;
	}

	public long tasksSubmitted() {
		return tasksSubmitted;
	}

	public long tasksCompleted() {
		return tasksCompleted;
	}
}
