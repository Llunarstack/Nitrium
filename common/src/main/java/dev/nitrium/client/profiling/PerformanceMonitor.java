package dev.nitrium.client.profiling;

import dev.nitrium.config.NitriumConfig;
import dev.nitrium.config.NitriumConfigManager;

/**
 * Aggregates per-frame CPU and GPU timings and classifies the active bottleneck.
 */
public final class PerformanceMonitor {
	private static PerformanceMonitor instance;

	private final RollingAverage fpsAverage = new RollingAverage(120);
	private final RollingAverage gpuMsAverage = new RollingAverage(120);
	private final RollingAverage tickCpuMsAverage = new RollingAverage(120);
	private final RollingAverage worldRenderCpuMsAverage = new RollingAverage(120);

	private long frameIndex;
	private FrameMetrics latestMetrics = FrameMetrics.empty(0);

	private long frameStartNs;
	private long tickStartNs;
	private long worldRenderStartNs;
	private long lastTickCpuNs;
	private long lastWorldRenderCpuNs;
	private long lastGpuWorldRenderNs;

	private PerformanceMonitor() {
	}

	public static PerformanceMonitor get() {
		if (instance == null) {
			instance = new PerformanceMonitor();
		}
		return instance;
	}

	public void onFrameStart() {
		frameStartNs = System.nanoTime();
	}

	public void onClientTickStart() {
		tickStartNs = System.nanoTime();
	}

	public void onClientTickEnd() {
		if (tickStartNs != 0L) {
			lastTickCpuNs = System.nanoTime() - tickStartNs;
		}
	}

	public void onWorldRenderStart() {
		worldRenderStartNs = System.nanoTime();
	}

	public void onWorldRenderEnd() {
		if (worldRenderStartNs != 0L) {
			lastWorldRenderCpuNs = System.nanoTime() - worldRenderStartNs;
		}
	}

	public void onGpuSample(long gpuWorldRenderNs) {
		if (gpuWorldRenderNs >= 0L) {
			lastGpuWorldRenderNs = gpuWorldRenderNs;
		}
	}

	public void onFrameEnd() {
		frameIndex++;

		long totalFrameNs = frameStartNs == 0L ? 0L : System.nanoTime() - frameStartNs;
		float instantaneousFps = totalFrameNs > 0L ? 1_000_000_000.0f / totalFrameNs : 0.0f;
		BottleneckType bottleneck = classifyBottleneck(lastTickCpuNs, lastWorldRenderCpuNs, lastGpuWorldRenderNs);

		latestMetrics = new FrameMetrics(
				frameIndex,
				totalFrameNs,
				lastTickCpuNs,
				lastWorldRenderCpuNs,
				lastGpuWorldRenderNs,
				bottleneck,
				instantaneousFps
		);

		if (totalFrameNs > 0L) {
			fpsAverage.add(instantaneousFps);
		}
		if (lastGpuWorldRenderNs > 0L) {
			gpuMsAverage.add(lastGpuWorldRenderNs / 1_000_000.0);
		}
		if (lastTickCpuNs > 0L) {
			tickCpuMsAverage.add(lastTickCpuNs / 1_000_000.0);
		}
		if (lastWorldRenderCpuNs > 0L) {
			worldRenderCpuMsAverage.add(lastWorldRenderCpuNs / 1_000_000.0);
		}
	}

	public FrameMetrics latestMetrics() {
		return latestMetrics;
	}

	public double averageFps() {
		return fpsAverage.average();
	}

	public double averageGpuMs() {
		return gpuMsAverage.average();
	}

	public double averageTickCpuMs() {
		return tickCpuMsAverage.average();
	}

	public double averageWorldRenderCpuMs() {
		return worldRenderCpuMsAverage.average();
	}

	public BottleneckType dominantBottleneck() {
		return latestMetrics.bottleneck();
	}

	private BottleneckType classifyBottleneck(long tickCpuNs, long worldRenderCpuNs, long gpuWorldRenderNs) {
		NitriumConfig config = NitriumConfigManager.get();
		double targetFrameNs = 1_000_000_000.0 / Math.max(1, config.targetFps);
		double gpuBudgetNs = targetFrameNs * config.gpuBoundBudgetFraction;
		double cpuBudgetNs = targetFrameNs * config.cpuBoundBudgetFraction;

		double cpuNs = tickCpuNs + Math.max(0L, worldRenderCpuNs - gpuWorldRenderNs);
		double gpuNs = gpuWorldRenderNs > 0L ? gpuWorldRenderNs : worldRenderCpuNs;

		boolean gpuBound = gpuNs >= gpuBudgetNs && gpuNs > cpuNs * 1.15;
		boolean cpuBound = cpuNs >= cpuBudgetNs && cpuNs > gpuNs * 1.15;

		if (gpuBound) {
			return BottleneckType.GPU_BOUND;
		}
		if (cpuBound) {
			return BottleneckType.CPU_BOUND;
		}
		if (gpuNs > 0.0 || cpuNs > 0.0) {
			return BottleneckType.BALANCED;
		}
		return BottleneckType.UNKNOWN;
	}
}
