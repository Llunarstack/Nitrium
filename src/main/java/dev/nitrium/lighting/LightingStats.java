package dev.nitrium.lighting;

/**
 * Lighting engine profiling counters.
 */
public final class LightingStats {
	private long updates;
	private long batches;
	private long propagatedVoxels;

	public void recordUpdate() {
		updates++;
	}

	public void recordBatch(int mergedCount) {
		batches++;
	}

	public void recordPropagation(long voxels) {
		propagatedVoxels += voxels;
	}

	public void reset() {
		updates = 0;
		batches = 0;
		propagatedVoxels = 0;
	}

	public long updates() {
		return updates;
	}

	public long batches() {
		return batches;
	}

	public long propagatedVoxels() {
		return propagatedVoxels;
	}
}
