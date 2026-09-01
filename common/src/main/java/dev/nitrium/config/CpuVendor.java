package dev.nitrium.config;

/**
 * Detected CPU vendor for SIMD and thread-pool tuning.
 */
public enum CpuVendor {
	UNKNOWN,
	INTEL,
	AMD;

	public static CpuVendor fromNativeId(int id) {
		return switch (id) {
			case 1 -> INTEL;
			case 2 -> AMD;
			default -> UNKNOWN;
		};
	}
}
