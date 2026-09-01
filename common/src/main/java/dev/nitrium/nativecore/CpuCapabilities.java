package dev.nitrium.nativecore;

import dev.nitrium.Nitrium;
import dev.nitrium.config.CpuVendor;

/**
 * Runtime CPU capabilities probed via native CPUID or Java fallbacks.
 */
public final class CpuCapabilities {
	private static CpuCapabilities instance;

	private final CpuVendor vendor;
	private final int logicalCores;
	private final int physicalCores;
	private final boolean avx2;
	private final boolean avx512;
	private final long maxHeapMb;

	private CpuCapabilities(
			CpuVendor vendor,
			int logicalCores,
			int physicalCores,
			boolean avx2,
			boolean avx512,
			long maxHeapMb
	) {
		this.vendor = vendor;
		this.logicalCores = logicalCores;
		this.physicalCores = physicalCores;
		this.avx2 = avx2;
		this.avx512 = avx512;
		this.maxHeapMb = maxHeapMb;
	}

	public static void probe() {
		if (instance != null) {
			return;
		}

		CpuVendor vendor = CpuVendor.UNKNOWN;
		int logical = Runtime.getRuntime().availableProcessors();
		int physical = logical;
		boolean avx2 = false;
		boolean avx512 = false;

		if (NitriumNativeLoader.isAvailable()) {
			vendor = CpuVendor.fromNativeId(NitriumNative.cpuVendor());
			logical = Math.max(1, NitriumNative.cpuLogicalCores());
			physical = Math.max(1, NitriumNative.cpuPhysicalCores());
			avx2 = NitriumNative.hasAvx2();
			avx512 = NitriumNative.hasAvx512();
		}

		long maxHeapMb = Runtime.getRuntime().maxMemory() / (1024L * 1024L);
		instance = new CpuCapabilities(vendor, logical, physical, avx2, avx512, maxHeapMb);

		Nitrium.LOGGER.info(
				"CPU probe: {} ({} physical / {} logical cores, AVX2={}, AVX-512={}, heap={} MB)",
				vendor,
				physical,
				logical,
				avx2,
				avx512,
				maxHeapMb
		);
	}

	public static CpuCapabilities get() {
		if (instance == null) {
			probe();
		}
		return instance;
	}

	public CpuVendor vendor() {
		return vendor;
	}

	public int logicalCores() {
		return logicalCores;
	}

	public int physicalCores() {
		return physicalCores;
	}

	public boolean hasAvx2() {
		return avx2;
	}

	public boolean hasAvx512() {
		return avx512;
	}

	public long maxHeapMb() {
		return maxHeapMb;
	}

	/** Worker thread count clamped to physical cores, leaving headroom for the game thread. */
	public int recommendedWorkerThreads(int configured, int reservedCores) {
		int maxWorkers = Math.max(1, physicalCores - reservedCores);
		return Math.clamp(configured, 1, maxWorkers);
	}
}
