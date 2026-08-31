package dev.nitrium.layout;

/**
 * Memory layout optimization profiling counters.
 */
public final class LayoutStats {
	private long sectionsAllocated;

	public void recordSectionAllocated() {
		sectionsAllocated++;
	}

	public void reset() {
		sectionsAllocated = 0;
	}

	public long sectionsAllocated() {
		return sectionsAllocated;
	}
}
