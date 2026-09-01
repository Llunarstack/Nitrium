package dev.nitrium.client.governor;

import dev.nitrium.client.nativegl.NitriumAzdoBackend;
import dev.nitrium.client.profiling.BottleneckType;
import dev.nitrium.client.profiling.PerformanceMonitor;
import dev.nitrium.config.NitriumConfig;
import dev.nitrium.config.NitriumConfigManager;

/**
 * Adaptive quality governor. Two loops run off the profiler: a per-tick fast loop nudges render
 * scale and render distance, and a stability-gated slow loop steps shader quality up or down with
 * hysteresis so shader recompiles don't thrash. The AZDO/Iris bridge and the debug overlay read the
 * resolved outputs back out.
 */
public final class QualityGovernor {
	private static QualityGovernor instance;

	private final ShaderQualityController shaderController = new ShaderQualityController();
	private final AdaptiveRenderDistanceController renderDistanceController = new AdaptiveRenderDistanceController();
	private final DynamicResolutionController resolutionController = new DynamicResolutionController();
	private final VanillaGraphicsBridge vanillaGraphics = new VanillaGraphicsBridge();
	private final IrisShaderBridge irisBridge = new IrisShaderBridge();

	private int slowLoopTickCounter;
	private float currentRenderScale = 1.0f;
	private int renderDistanceChunks = -1;
	private volatile boolean irisGoverned;

	private QualityGovernor() {
	}

	public static QualityGovernor get() {
		if (instance == null) {
			instance = new QualityGovernor();
		}
		return instance;
	}

	public void onClientTick() {
		fastLoop();
		slowLoopTick();
	}

	private void fastLoop() {
		NitriumConfig config = NitriumConfigManager.get();
		PerformanceMonitor monitor = PerformanceMonitor.get();

		double averageFps = monitor.averageFps();
		double averageGpuMs = monitor.averageGpuMs();
		BottleneckType bottleneck = monitor.dominantBottleneck();

		// Cache once per tick so per-entity culling can read it without repeated reflection.
		irisGoverned = irisBridge.isShaderPackInUse();

		// Resolution scaling reacts to GPU pressure; its floor is set by the active shader rung.
		float scaleFloor = shaderController.currentProfile().renderScaleFloor();
		currentRenderScale = resolutionController.evaluate(config, averageGpuMs, scaleFloor);

		// Feed the recommended scale into the AZDO framebuffer path (advisory on vanilla).
		NitriumAzdoBackend backend = NitriumAzdoBackend.get();
		if (backend != null) {
			backend.onRenderScale(currentRenderScale);
		}

		// Render distance is read-only: changing the option forces a full chunk reload.
		if (config.enableAdaptiveRenderDistance) {
			renderDistanceChunks = renderDistanceController.evaluate(config, averageFps, bottleneck, 1);
		}
	}

	private void slowLoopTick() {
		NitriumConfig config = NitriumConfigManager.get();
		slowLoopTickCounter++;

		int intervalTicks = 20 * Math.max(1, config.slowLoopIntervalSeconds);
		if (slowLoopTickCounter < intervalTicks) {
			return;
		}
		slowLoopTickCounter = 0;

		PerformanceMonitor monitor = PerformanceMonitor.get();
		boolean levelChanged = shaderController.evaluate(
				config,
				config.slowLoopIntervalSeconds,
				monitor.averageFps(),
				monitor.dominantBottleneck()
		);

		if (levelChanged) {
			ShaderProfile profile = shaderController.currentProfile();
			// When an Iris shader pack owns the look, govern it through Iris; otherwise reflect the
			// level onto vanilla graphics options. Never fight Iris with the vanilla graphics preset.
			if (irisBridge.isShaderPackInUse()) {
				irisBridge.apply(profile);
			} else {
				vanillaGraphics.apply(profile);
			}
		}
	}

	public float currentRenderScale() {
		return currentRenderScale;
	}

	public void setCurrentRenderScale(float scale) {
		NitriumConfig config = NitriumConfigManager.get();
		this.currentRenderScale = Math.clamp(scale, config.renderScaleMin, config.renderScaleMax);
	}

	public ShaderProfile shaderProfile() {
		return shaderController.currentProfile();
	}

	public ShaderQualityLevel shaderLevel() {
		return shaderController.currentLevel();
	}

	public ShaderQualityLevel shaderCeiling() {
		return shaderController.hardwareCeiling();
	}

	public int renderDistanceChunks() {
		return renderDistanceChunks;
	}

	/**
	 * True when an Iris shader pack is active (cached each tick). Shader packs roughly double
	 * geometry cost — the world is drawn again for the shadow pass — so culling should be tighter.
	 */
	public boolean isIrisGoverned() {
		return irisGoverned;
	}

	public void reset() {
		shaderController.reset();
		renderDistanceController.reset();
		resolutionController.reset();
		vanillaGraphics.reset();
		irisBridge.reset();
		slowLoopTickCounter = 0;
		currentRenderScale = 1.0f;
		renderDistanceChunks = -1;
	}
}
