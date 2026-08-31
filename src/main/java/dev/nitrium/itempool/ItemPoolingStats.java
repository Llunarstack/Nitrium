package dev.nitrium.itempool;

/**
 * Item/XP pooling profiling counters.
 */
public final class ItemPoolingStats {
	private long orbMerges;
	private long itemMerges;

	public void recordOrbMerge(int count) {
		orbMerges += count;
	}

	public void recordItemMerge(int count) {
		itemMerges += count;
	}

	public void reset() {
		orbMerges = 0;
		itemMerges = 0;
	}

	public long orbMerges() {
		return orbMerges;
	}

	public long itemMerges() {
		return itemMerges;
	}
}
