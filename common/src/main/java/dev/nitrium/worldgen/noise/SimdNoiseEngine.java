package dev.nitrium.worldgen.noise;

import dev.nitrium.config.NitriumConfigManager;
import dev.nitrium.nativecore.NitriumNativeLoader;
import dev.nitrium.nativecore.SimdNoise3D;

/**
 * Java facade over native SIMD 3D noise with scalar fallback.
 */
public final class SimdNoiseEngine {
	public void fillCoarse(
			float[] out,
			int sizeX,
			int sizeY,
			int sizeZ,
			float originX,
			float originY,
			float originZ,
			float stepX,
			float stepY,
			float stepZ,
			int seed
	) {
		if (NitriumConfigManager.get().enableSimdNoise && NitriumNativeLoader.isAvailable()) {
			SimdNoise3D.fillCoarseGrid(out, sizeX, sizeY, sizeZ, originX, originY, originZ, stepX, stepY, stepZ, seed);
		} else {
			SimdNoise3D.fillCoarseGridJava(out, sizeX, sizeY, sizeZ, originX, originY, originZ, stepX, stepY, stepZ, seed);
		}
	}

	public float sampleFullResolution(int worldX, int worldY, int worldZ, int seed) {
		float[] single = new float[1];
		fillCoarse(single, 1, 1, 1, worldX, worldY, worldZ, 1.0f, 1.0f, 1.0f, seed);
		return single[0];
	}
}
