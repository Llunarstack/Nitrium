package dev.nitrium.config;

/**
 * Coarse hardware tier derived from GPU vendor, VRAM, and integration class.
 * Drives default quality ceilings and memory budgets.
 */
public enum HardwareProfile {
	POTATO(512, 48, 64, 8192, 2, 48),
	MID(1024, 96, 128, 32768, 4, 96),
	GOD(2048, 160, 256, 65536, 8, 128);

	private final int maxShadowMapResolution;
	private final int maxShadowDistanceBlocks;
	private final int geometryBufferBudgetMb;
	private final int maxGpuParticles;
	private final int maxConcurrentMeshTasks;
	private final int maxEntityRenderDistanceBlocks;

	HardwareProfile(
			int maxShadowMapResolution,
			int maxShadowDistanceBlocks,
			int geometryBufferBudgetMb,
			int maxGpuParticles,
			int maxConcurrentMeshTasks,
			int maxEntityRenderDistanceBlocks
	) {
		this.maxShadowMapResolution = maxShadowMapResolution;
		this.maxShadowDistanceBlocks = maxShadowDistanceBlocks;
		this.geometryBufferBudgetMb = geometryBufferBudgetMb;
		this.maxGpuParticles = maxGpuParticles;
		this.maxConcurrentMeshTasks = maxConcurrentMeshTasks;
		this.maxEntityRenderDistanceBlocks = maxEntityRenderDistanceBlocks;
	}

	public int maxShadowMapResolution() {
		return maxShadowMapResolution;
	}

	public int maxShadowDistanceBlocks() {
		return maxShadowDistanceBlocks;
	}

	public int geometryBufferBudgetMb() {
		return geometryBufferBudgetMb;
	}

	public int maxGpuParticles() {
		return maxGpuParticles;
	}

	public int maxConcurrentMeshTasks() {
		return maxConcurrentMeshTasks;
	}

	public int maxConcurrentCacheReads() {
		return maxConcurrentMeshTasks * 2;
	}

	public int maxEntityRenderDistanceBlocks() {
		return maxEntityRenderDistanceBlocks;
	}

	public static HardwareProfile fromVramMb(int dedicatedVramMb) {
		return resolve(GpuVendor.UNKNOWN, dedicatedVramMb, false, "");
	}

	public static HardwareProfile resolve(
			GpuVendor vendor,
			int dedicatedVramMb,
			boolean integrated,
			String renderer
	) {
		if (integrated || vendor == GpuVendor.INTEL && isIntelIntegratedRenderer(renderer)) {
			return dedicatedVramMb <= 1024 ? POTATO : MID;
		}

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

	private static boolean isIntelIntegratedRenderer(String renderer) {
		if (renderer == null || renderer.isBlank()) {
			return true;
		}
		String lower = renderer.toLowerCase();
		return !lower.contains("arc");
	}
}
