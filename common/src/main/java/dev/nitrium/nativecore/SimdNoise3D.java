package dev.nitrium.nativecore;

/**
 * AVX2-accelerated 3D noise sampling and trilinear coarse-grid interpolation.
 * Falls back to scalar Java when native is unavailable.
 */
public final class SimdNoise3D {
	private SimdNoise3D() {
	}

	public static void fillCoarseGrid(
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
		if (NitriumNativeLoader.isAvailable()) {
			nativeFillCoarse(out, sizeX, sizeY, sizeZ, originX, originY, originZ, stepX, stepY, stepZ, seed);
			return;
		}

		javaFillCoarse(out, sizeX, sizeY, sizeZ, originX, originY, originZ, stepX, stepY, stepZ, seed);
	}

	public static void trilinearFill(
			float[] out,
			int chunkX,
			int chunkY,
			int chunkZ,
			float[] coarse,
			int coarseX,
			int coarseY,
			int coarseZ,
			int stepX,
			int stepY,
			int stepZ
	) {
		if (NitriumNativeLoader.isAvailable()) {
			nativeTrilinearFill(out, chunkX, chunkY, chunkZ, coarse, coarseX, coarseY, coarseZ, stepX, stepY, stepZ);
			return;
		}

		javaTrilinearFill(out, chunkX, chunkY, chunkZ, coarse, coarseX, coarseY, coarseZ, stepX, stepY, stepZ);
	}

	public static void markHighGradient(
			byte[] mask,
			int chunkX,
			int chunkY,
			int chunkZ,
			float[] density,
			float threshold
	) {
		if (NitriumNativeLoader.isAvailable()) {
			nativeMarkHighGradient(mask, chunkX, chunkY, chunkZ, density, threshold);
			return;
		}

		javaMarkHighGradient(mask, chunkX, chunkY, chunkZ, density, threshold);
	}

	private static void javaFillCoarse(
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
		int index = 0;
		for (int z = 0; z < sizeZ; z++) {
			for (int y = 0; y < sizeY; y++) {
				for (int x = 0; x < sizeX; x++) {
					float wx = originX + x * stepX;
					float wy = originY + y * stepY;
					float wz = originZ + z * stepZ;
					out[index++] = valueNoise(wx, wy, wz, seed);
				}
			}
		}
	}

	private static void javaTrilinearFill(
			float[] out,
			int chunkX,
			int chunkY,
			int chunkZ,
			float[] coarse,
			int coarseX,
			int coarseY,
			int coarseZ,
			int stepX,
			int stepY,
			int stepZ
	) {
		int plane = chunkX * chunkY;
		for (int z = 0; z < chunkZ; z++) {
			int gz0 = z / stepZ;
			int gz1 = gz0 + 1 < coarseZ ? gz0 + 1 : gz0;
			float tz = (float) z / stepZ - gz0;

			for (int y = 0; y < chunkY; y++) {
				int gy0 = y / stepY;
				int gy1 = gy0 + 1 < coarseY ? gy0 + 1 : gy0;
				float ty = (float) y / stepY - gy0;

				for (int x = 0; x < chunkX; x++) {
					int gx0 = x / stepX;
					int gx1 = gx0 + 1 < coarseX ? gx0 + 1 : gx0;
					float tx = (float) x / stepX - gx0;

					float c000 = sample(coarse, gx0, gy0, gz0, coarseX, coarseY);
					float c100 = sample(coarse, gx1, gy0, gz0, coarseX, coarseY);
					float c010 = sample(coarse, gx0, gy1, gz0, coarseX, coarseY);
					float c110 = sample(coarse, gx1, gy1, gz0, coarseX, coarseY);
					float c001 = sample(coarse, gx0, gy0, gz1, coarseX, coarseY);
					float c101 = sample(coarse, gx1, gy0, gz1, coarseX, coarseY);
					float c011 = sample(coarse, gx0, gy1, gz1, coarseX, coarseY);
					float c111 = sample(coarse, gx1, gy1, gz1, coarseX, coarseY);

					float x00 = lerp(c000, c100, tx);
					float x10 = lerp(c010, c110, tx);
					float x01 = lerp(c001, c101, tx);
					float x11 = lerp(c011, c111, tx);
					float y0 = lerp(x00, x10, ty);
					float y1 = lerp(x01, x11, ty);
					out[x + y * chunkX + z * plane] = lerp(y0, y1, tz);
				}
			}
		}
	}

	private static void javaMarkHighGradient(
			byte[] mask,
			int chunkX,
			int chunkY,
			int chunkZ,
			float[] density,
			float threshold
	) {
		int plane = chunkX * chunkY;
		for (int z = 1; z < chunkZ - 1; z++) {
			for (int y = 1; y < chunkY - 1; y++) {
				for (int x = 1; x < chunkX - 1; x++) {
					int i = x + y * chunkX + z * plane;
					float dx = Math.abs(density[i + 1] - density[i - 1]);
					float dy = Math.abs(density[i + chunkX] - density[i - chunkX]);
					float dz = Math.abs(density[i + plane] - density[i - plane]);
					if (dx + dy + dz > threshold) {
						mask[i] = 1;
					}
				}
			}
		}
	}

	private static float sample(float[] grid, int gx, int gy, int gz, int sx, int sy) {
		return grid[gx + gy * sx + gz * sx * sy];
	}

	private static float lerp(float a, float b, float t) {
		return a + t * (b - a);
	}

	private static float valueNoise(float x, float y, float z, int seed) {
		int xi = (int) Math.floor(x) & 255;
		int yi = (int) Math.floor(y) & 255;
		int zi = (int) Math.floor(z) & 255;
		float xf = x - (float) Math.floor(x);
		float yf = y - (float) Math.floor(y);
		float zf = z - (float) Math.floor(z);
		float u = fade(xf);
		float v = fade(yf);
		float w = fade(zf);

		float x00 = lerp(hash(xi, yi, zi, seed), hash(xi + 1, yi, zi, seed), u);
		float x10 = lerp(hash(xi, yi + 1, zi, seed), hash(xi + 1, yi + 1, zi, seed), u);
		float x01 = lerp(hash(xi, yi, zi + 1, seed), hash(xi + 1, yi, zi + 1, seed), u);
		float x11 = lerp(hash(xi, yi + 1, zi + 1, seed), hash(xi + 1, yi + 1, zi + 1, seed), u);
		float y0 = lerp(x00, x10, v);
		float y1 = lerp(x01, x11, v);
		return lerp(y0, y1, w);
	}

	private static float fade(float t) {
		return t * t * t * (t * (t * 6.0f - 15.0f) + 10.0f);
	}

	private static float hash(int a, int b, int c, int seed) {
		int h = seed;
		h ^= a * 374761393;
		h ^= b * 668265263;
		h ^= (int) ((long) c * 2246822519L);
		h = (h ^ (h >>> 13)) * 1274126177;
		return ((h & 1023) / 512.0f) - 1.0f;
	}

	private static native void nativeFillCoarse(
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
	);

	private static native void nativeTrilinearFill(
			float[] out,
			int chunkX,
			int chunkY,
			int chunkZ,
			float[] coarse,
			int coarseX,
			int coarseY,
			int coarseZ,
			int stepX,
			int stepY,
			int stepZ
	);

	private static native void nativeMarkHighGradient(
			byte[] mask,
			int chunkX,
			int chunkY,
			int chunkZ,
			float[] density,
			float threshold
	);
}
