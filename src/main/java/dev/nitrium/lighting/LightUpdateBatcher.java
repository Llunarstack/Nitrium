package dev.nitrium.lighting;

/**
 * Merges overlapping light update regions into a single batch propagation pass.
 */
public final class LightUpdateBatcher {
	private int minX = Integer.MAX_VALUE;
	private int minY = Integer.MAX_VALUE;
	private int minZ = Integer.MAX_VALUE;
	private int maxX = Integer.MIN_VALUE;
	private int maxY = Integer.MIN_VALUE;
	private int maxZ = Integer.MIN_VALUE;
	private int pendingUpdates;
	private int scheduledTick = -1;

	public void include(int x, int y, int z) {
		minX = Math.min(minX, x);
		minY = Math.min(minY, y);
		minZ = Math.min(minZ, z);
		maxX = Math.max(maxX, x);
		maxY = Math.max(maxY, y);
		maxZ = Math.max(maxZ, z);
		pendingUpdates++;
	}

	public boolean hasPending() {
		return pendingUpdates > 0;
	}

	public int pendingCount() {
		return pendingUpdates;
	}

	public int scheduledTick() {
		return scheduledTick;
	}

	public void scheduleForTick(int tick) {
		scheduledTick = tick;
	}

	public boolean isDue(int currentTick) {
		return scheduledTick >= 0 && currentTick >= scheduledTick;
	}

	public LightUpdateRegion flush() {
		if (!hasPending()) {
			return null;
		}
		LightUpdateRegion region = new LightUpdateRegion(minX, minY, minZ, maxX, maxY, maxZ, pendingUpdates);
		reset();
		return region;
	}

	public void reset() {
		minX = Integer.MAX_VALUE;
		minY = Integer.MAX_VALUE;
		minZ = Integer.MAX_VALUE;
		maxX = Integer.MIN_VALUE;
		maxY = Integer.MIN_VALUE;
		maxZ = Integer.MIN_VALUE;
		pendingUpdates = 0;
		scheduledTick = -1;
	}
}
