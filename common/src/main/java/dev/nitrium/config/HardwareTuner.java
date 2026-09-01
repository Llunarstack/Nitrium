package dev.nitrium.config;

import dev.nitrium.Nitrium;
import dev.nitrium.client.profiling.GpuCapabilities;
import dev.nitrium.nativecore.CpuCapabilities;
import dev.nitrium.nativecore.NitriumNativeLoader;

/**
 * Applies hardware-aware defaults to {@link NitriumConfig} once per session. Respects user overrides
 * in nitrium.json — only tightens budgets on weak hardware, never loosens them beyond configured max.
 */
public final class HardwareTuner {
	private static boolean tuned;

	private HardwareTuner() {
	}

	public static void tuneServer() {
		if (tuned || !NitriumConfigManager.get().enableHardwareAutoTune) {
			return;
		}

		CpuCapabilities cpu = CpuCapabilities.get();
		NitriumConfig config = NitriumConfigManager.get();

		config.worldgenWorkerThreads = cpu.recommendedWorkerThreads(config.worldgenWorkerThreads, 2);
		config.lightWorkerThreads = cpu.recommendedWorkerThreads(config.lightWorkerThreads, 2);
		config.parallelTickWorkerThreads = cpu.recommendedWorkerThreads(config.parallelTickWorkerThreads, 1);
		config.networkWorkerThreads = cpu.recommendedWorkerThreads(config.networkWorkerThreads, 1);
		config.maxConcurrentChunkWrites = cpu.recommendedWorkerThreads(config.maxConcurrentChunkWrites, 1);

		if (cpu.maxHeapMb() > 0 && cpu.maxHeapMb() < 4096) {
			config.chunkSaveRingBufferMb = Math.min(config.chunkSaveRingBufferMb, 32);
			config.itemMergeRadiusBlocks = Math.min(config.itemMergeRadiusBlocks, 2);
		}

		if (!cpu.hasAvx2()) {
			config.enableSimdNoise = false;
		}

		if (!NitriumNativeLoader.isAvailable()) {
			config.enableNativePacketCompression = false;
		}

		tuned = true;
		Nitrium.LOGGER.info(
				"Nitrium server hardware tune: worldgenWorkers={}, lightWorkers={}, simdNoise={}",
				config.worldgenWorkerThreads,
				config.lightWorkerThreads,
				config.enableSimdNoise
		);
	}

	public static void tuneClient(GpuCapabilities gpu) {
		if (!NitriumConfigManager.get().enableHardwareAutoTune) {
			return;
		}

		NitriumConfig config = NitriumConfigManager.get();
		HardwareProfile profile = gpu.hardwareProfile();

		config.geometryBufferBudgetMb = profile.geometryBufferBudgetMb();
		config.maxGpuParticles = profile.maxGpuParticles();
		config.maxConcurrentMeshTasks = profile.maxConcurrentMeshTasks();
		config.maxConcurrentCacheReads = profile.maxConcurrentCacheReads();
		config.maxEntityRenderDistanceBlocks = Math.min(
				config.maxEntityRenderDistanceBlocks,
				profile.maxEntityRenderDistanceBlocks()
		);
		config.shadowCullDistanceBlocks = Math.min(
				config.shadowCullDistanceBlocks,
				profile.maxShadowDistanceBlocks()
		);

		switch (gpu.vendor()) {
			case INTEL -> {
				if (gpu.integratedGpu()) {
					config.entityCullStressFactor = Math.min(config.entityCullStressFactor, 0.55f);
					config.renderScaleMax = Math.min(config.renderScaleMax, 0.9f);
					config.enableHiZOcclusion = false;
					config.enableGpuParticles = false;
				}
			}
			case AMD -> {
				config.maxConcurrentMeshTasks = Math.min(config.maxConcurrentMeshTasks + 1, 6);
			}
			case NVIDIA -> {
				// Discrete NVIDIA: keep configured ceilings; native compression is usually a win.
			}
			default -> {
			}
		}

		if (gpu.dedicatedVramMb() > 0 && gpu.dedicatedVramMb() < 3072) {
			config.geometryBufferBudgetMb = Math.min(config.geometryBufferBudgetMb, 128);
			config.maxGpuParticles = Math.min(config.maxGpuParticles, 16384);
		}

		Nitrium.LOGGER.info(
				"Nitrium client hardware tune: profile={}, vendor={}, vram={} MB, geomBudget={} MB, particles={}",
				profile,
				gpu.vendor(),
				gpu.dedicatedVramMb() > 0 ? gpu.dedicatedVramMb() : "unknown",
				config.geometryBufferBudgetMb,
				config.maxGpuParticles
		);
	}
}
