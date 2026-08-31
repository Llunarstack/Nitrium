package dev.nitrium.client.audio;

/**
 * Async audio occlusion profiling counters.
 */
public final class AudioStats {
	private long queriesSubmitted;
	private long queriesCompleted;
	private long cacheHits;

	public void recordQuerySubmitted() {
		queriesSubmitted++;
	}

	public void recordQueryCompleted() {
		queriesCompleted++;
	}

	public void recordCacheHit() {
		cacheHits++;
	}

	public void reset() {
		queriesSubmitted = 0;
		queriesCompleted = 0;
		cacheHits = 0;
	}

	public long queriesSubmitted() {
		return queriesSubmitted;
	}

	public long queriesCompleted() {
		return queriesCompleted;
	}

	public long cacheHits() {
		return cacheHits;
	}
}
