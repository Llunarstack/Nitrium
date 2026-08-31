package dev.nitrium.client.profiling;

import dev.nitrium.config.NitriumConfigManager;
import dev.nitrium.Nitrium;
import dev.nitrium.client.governor.QualityGovernor;
import dev.nitrium.client.platform.ClientEvents;
import dev.nitrium.platform.Platform;
import net.minecraft.client.Minecraft;

/**
 * Registers profiling hooks on the client tick and world render boundaries.
 */
public final class NitriumProfiler {
	private static NitriumProfiler instance;

	private final GpuTimerQuery worldRenderGpuQuery = new GpuTimerQuery();
	private boolean gpuProfilingEnabled;
	private boolean irisLoaded;
	private boolean gpuProbed;

	private NitriumProfiler() {
	}

	public static void init() {
		if (instance != null) {
			return;
		}

		instance = new NitriumProfiler();
		instance.register();
	}

	private void register() {
		irisLoaded = Platform.isModLoaded("iris");
		gpuProfilingEnabled = NitriumConfigManager.get().enableGpuProfiling;

		ClientEvents events = ClientEvents.get();

		events.clientTickStart(client -> {
			PerformanceMonitor.get().onFrameStart();
			PerformanceMonitor.get().onClientTickStart();
		});

		events.clientTickEnd(client -> {
			PerformanceMonitor.get().onClientTickEnd();
			QualityGovernor.get().onClientTick();
		});

		events.worldRenderStart(() -> {
			if (!gpuProbed) {
				GpuCapabilities.probe();
				gpuProbed = true;
			}

			PerformanceMonitor.get().onWorldRenderStart();
			if (gpuProfilingEnabled) {
				worldRenderGpuQuery.begin();
			}
		});

		events.worldRenderEnd(() -> {
			if (gpuProfilingEnabled) {
				worldRenderGpuQuery.end();
				long gpuNs = worldRenderGpuQuery.pollNanoseconds();
				PerformanceMonitor.get().onGpuSample(gpuNs);
			}

			PerformanceMonitor.get().onWorldRenderEnd();
			PerformanceMonitor.get().onFrameEnd();
		});

		Nitrium.LOGGER.info(
				"Nitrium profiler active (irisLoaded={}, gpuProfiling={})",
				irisLoaded,
				gpuProfilingEnabled
		);
	}

	public static NitriumProfiler get() {
		return instance;
	}

	public boolean isIrisLoaded() {
		return irisLoaded;
	}

	public void setGpuProfilingEnabled(boolean enabled) {
		this.gpuProfilingEnabled = enabled;
		NitriumConfigManager.get().enableGpuProfiling = enabled;
		NitriumConfigManager.save();
	}
}
