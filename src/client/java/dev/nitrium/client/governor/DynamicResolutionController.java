package dev.nitrium.client.governor;

import dev.nitrium.config.NitriumConfig;

/**
 * Dynamic resolution driver. GPU fill cost scales with pixel count, i.e. roughly the square of the
 * linear render scale, so to pull a measured GPU time {@code g} at scale {@code s} down to budget
 * {@code b} the ideal next scale is {@code s * sqrt(b / g)}. That target is damped (deadband + max
 * step) to avoid resolution pumping and clamped to the config window and the shader rung's floor.
 * {@link #currentScale()} is only a recommendation — vanilla has no runtime resolution scaling, so
 * it does nothing until a Sodium/Iris framebuffer bridge consumes it.
 */
public final class DynamicResolutionController {
	private float currentScale = 1.0f;

	/**
	 * @param averageGpuMs rolling average GPU world-render time (ms); &le;0 means no GPU timer
	 * @param scaleFloor   lower bound imposed by the current shader quality rung
	 * @return the recommended linear render scale in {@code [renderScaleMin, renderScaleMax]}
	 */
	public float evaluate(NitriumConfig config, double averageGpuMs, float scaleFloor) {
		if (!config.enableDynamicResolution || averageGpuMs <= 0.0) {
			return currentScale;
		}

		double frameBudgetMs = 1000.0 / Math.max(1, config.targetFps);
		double gpuBudgetMs = frameBudgetMs * config.renderScaleGpuBudgetFraction;
		if (gpuBudgetMs <= 0.0) {
			return currentScale;
		}

		double error = averageGpuMs / gpuBudgetMs; // >1 = over budget, <1 = headroom
		if (Math.abs(error - 1.0) < config.renderScaleDeadband) {
			return currentScale; // inside deadband — hold to avoid pumping
		}

		double idealScale = currentScale / Math.sqrt(error);
		double delta = Math.clamp(idealScale - currentScale, -config.renderScaleMaxStep, config.renderScaleMaxStep);

		float low = Math.max(config.renderScaleMin, scaleFloor);
		currentScale = (float) Math.clamp(currentScale + delta, low, config.renderScaleMax);
		return currentScale;
	}

	public float currentScale() {
		return currentScale;
	}

	public void reset() {
		currentScale = 1.0f;
	}
}
