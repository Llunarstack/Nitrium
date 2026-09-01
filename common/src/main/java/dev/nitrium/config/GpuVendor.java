package dev.nitrium.config;

/**
 * Detected GPU vendor for vendor-specific tuning paths.
 */
public enum GpuVendor {
	UNKNOWN,
	NVIDIA,
	AMD,
	INTEL;

	public boolean isDiscreteFriendly() {
		return this == NVIDIA || this == AMD;
	}
}
