package dev.nitrium.nativecore;

/**
 * SIMD-accelerated frustum culling over Structure-of-Arrays entity bounds.
 * Falls back to scalar Java when the native library is unavailable.
 */
public final class SimdFrustumCuller {
	private static final int MAX_ENTITIES = 4096;

	private final float[] minX = new float[MAX_ENTITIES];
	private final float[] minY = new float[MAX_ENTITIES];
	private final float[] minZ = new float[MAX_ENTITIES];
	private final float[] maxX = new float[MAX_ENTITIES];
	private final float[] maxY = new float[MAX_ENTITIES];
	private final float[] maxZ = new float[MAX_ENTITIES];
	private final float[] planes = new float[24];

	private int count;

	private SimdFrustumCuller() {
	}

	public static SimdFrustumCuller create() {
		return new SimdFrustumCuller();
	}

	public void clear() {
		count = 0;
	}

	public int entityCount() {
		return count;
	}

	public void setFrustumPlanes(float[] frustumPlanes24) {
		if (frustumPlanes24.length != 24) {
			throw new IllegalArgumentException("Expected 6 planes × 4 floats");
		}
		System.arraycopy(frustumPlanes24, 0, planes, 0, 24);
	}

	public int addAabb(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
		if (count >= MAX_ENTITIES) {
			return -1;
		}

		int index = count++;
		this.minX[index] = minX;
		this.minY[index] = minY;
		this.minZ[index] = minZ;
		this.maxX[index] = maxX;
		this.maxY[index] = maxY;
		this.maxZ[index] = maxZ;
		return index;
	}

	public int cullVisibleMask() {
		if (count == 0) {
			return 0;
		}

		if (NitriumNativeLoader.isAvailable()) {
			return nativeCullSoa(minX, minY, minZ, maxX, maxY, maxZ, planes, count);
		}

		return javaCullFallback();
	}

	public boolean isVisible(int index, int mask) {
		return (mask & (1 << index)) != 0;
	}

	private int javaCullFallback() {
		int mask = 0;
		for (int i = 0; i < count; i++) {
			if (aabbVisible(minX[i], minY[i], minZ[i], maxX[i], maxY[i], maxZ[i])) {
				mask |= (1 << i);
			}
		}
		return mask;
	}

	private boolean aabbVisible(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
		for (int p = 0; p < 6; p++) {
			float a = planes[p * 4];
			float b = planes[p * 4 + 1];
			float c = planes[p * 4 + 2];
			float d = planes[p * 4 + 3];

			float px = a >= 0 ? maxX : minX;
			float py = b >= 0 ? maxY : minY;
			float pz = c >= 0 ? maxZ : minZ;

			if (a * px + b * py + c * pz + d < 0) {
				return false;
			}
		}
		return true;
	}

	private static native int nativeCullSoa(
			float[] minX,
			float[] minY,
			float[] minZ,
			float[] maxX,
			float[] maxY,
			float[] maxZ,
			float[] planes,
			int count
	);
}
