package dev.nitrium.config;

/**
 * Coarse hardware tier derived from detected GPU VRAM and capabilities.
 * Drives default quality ceilings in later governor phases.
 */
public enum HardwareProfile {
	POTATO(512, 48),
	MID(1024, 96),
	GOD(2048, 160);

	private final int maxShadowMapResolution;
	private final int maxShadowDistanceBlocks;

	HardwareProfile(int maxShadowMapResolution, int maxShadowDistanceBlocks) {
		this.maxShadowMapResolution = maxShadowMapResolution;
		this.maxShadowDistanceBlocks = maxShadowDistanceBlocks;
	}

	public int maxShadowMapResolution() {
		return maxShadowMapResolution;
	}

	public int maxShadowDistanceBlocks() {
		return maxShadowDistanceBlocks;
	}

	public static HardwareProfile fromVramMb(int dedicatedVramMb) {
		if (dedicatedVramMb <= 0) {
			return MID;
		}
		if (dedicatedVramMb <= 4096) {
			return POTATO;
		}
		if (dedicatedVramMb <= 8192) {
			return MID;
		}
		return GOD;
	}
}
