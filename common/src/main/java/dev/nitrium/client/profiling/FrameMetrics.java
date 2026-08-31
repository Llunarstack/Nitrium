package dev.nitrium.client.profiling;

/**
 * Snapshot of one frame's timing breakdown, in nanoseconds unless noted.
 */
public record FrameMetrics(
		long frameIndex,
		long totalFrameNs,
		long clientTickCpuNs,
		long worldRenderCpuNs,
		long gpuWorldRenderNs,
		BottleneckType bottleneck,
		float instantaneousFps
) {
	public static FrameMetrics empty(long frameIndex) {
		return new FrameMetrics(frameIndex, 0L, 0L, 0L, 0L, BottleneckType.UNKNOWN, 0.0f);
	}

	public double clientTickCpuMs() {
		return clientTickCpuNs / 1_000_000.0;
	}

	public double worldRenderCpuMs() {
		return worldRenderCpuNs / 1_000_000.0;
	}

	public double gpuWorldRenderMs() {
		return gpuWorldRenderNs / 1_000_000.0;
	}

	public double totalFrameMs() {
		return totalFrameNs / 1_000_000.0;
	}
}
