package dev.nitrium.lighting;

/**
 * Axis-aligned bounding box for a batched light propagation pass.
 */
public record LightUpdateRegion(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, int updateCount) {
	public int volumeEstimate() {
		return (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
	}
}
