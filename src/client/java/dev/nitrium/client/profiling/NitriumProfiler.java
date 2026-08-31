package dev.nitrium.client.profiling;

import dev.nitrium.config.NitriumConfigManager;
import dev.nitrium.NitriumMod;
import dev.nitrium.client.governor.QualityGovernor;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.loader.api.FabricLoader;
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
		irisLoaded = FabricLoader.getInstance().isModLoaded("iris");
		gpuProfilingEnabled = NitriumConfigManager.get().enableGpuProfiling;

		ClientTickEvents.START_CLIENT_TICK.register(client -> {
			PerformanceMonitor.get().onFrameStart();
			PerformanceMonitor.get().onClientTickStart();
		});

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			PerformanceMonitor.get().onClientTickEnd();
			QualityGovernor.get().onClientTick();
		});

		WorldRenderEvents.START_MAIN.register(context -> {
			if (!gpuProbed) {
				GpuCapabilities.probe();
				gpuProbed = true;
			}

			PerformanceMonitor.get().onWorldRenderStart();
			if (gpuProfilingEnabled) {
				worldRenderGpuQuery.begin();
			}
		});

		WorldRenderEvents.END_MAIN.register(context -> {
			if (gpuProfilingEnabled) {
				worldRenderGpuQuery.end();
				long gpuNs = worldRenderGpuQuery.pollNanoseconds();
				PerformanceMonitor.get().onGpuSample(gpuNs);
			}

			PerformanceMonitor.get().onWorldRenderEnd();
			PerformanceMonitor.get().onFrameEnd();
		});

		NitriumMod.LOGGER.info(
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
