package dev.nitrium.worldgen.noise;

import dev.nitrium.config.NitriumConfigManager;

/**
 * Coarse-grid resolution for 3D density sampling (default 4×8×4).
 */
public final class CoarseGridConfig {
	public final int stepX;
	public final int stepY;
	public final int stepZ;
	public final float highGradientThreshold;

	public CoarseGridConfig(int stepX, int stepY, int stepZ, float highGradientThreshold) {
		this.stepX = stepX;
		this.stepY = stepY;
		this.stepZ = stepZ;
		this.highGradientThreshold = highGradientThreshold;
	}

	public static CoarseGridConfig fromConfig() {
		var config = NitriumConfigManager.get();
		return new CoarseGridConfig(
				config.worldgenCoarseStepXz,
				config.worldgenCoarseStepY,
				config.worldgenCoarseStepXz,
				config.worldgenHighGradientThreshold
		);
	}

	public int coarseSizeX(int chunkSizeX) {
		return chunkSizeX / stepX + 1;
	}

	public int coarseSizeY(int chunkSizeY) {
		return chunkSizeY / stepY + 1;
	}

	public int coarseSizeZ(int chunkSizeZ) {
		return chunkSizeZ / stepZ + 1;
	}

	public int coarseSampleCount(int chunkSizeX, int chunkSizeY, int chunkSizeZ) {
		return coarseSizeX(chunkSizeX) * coarseSizeY(chunkSizeY) * coarseSizeZ(chunkSizeZ);
	}
}
