package dev.nitrium.client.governor;

import dev.nitrium.Nitrium;
import dev.nitrium.client.profiling.BottleneckType;
import dev.nitrium.client.profiling.GpuCapabilities;
import dev.nitrium.config.NitriumConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;

/**
 * Grows Minecraft's render distance a chunk at a time while FPS holds above target, and shrinks it
 * when FPS drops below a floor. {@code LevelRenderer} notices the changed option each frame and
 * rebuilds on its own, so a plain {@link OptionInstance#set} is enough. Bounds come from config and
 * the upper one is trimmed on low-VRAM GPUs; a cooldown after each change lets the rebuild settle.
 */
public final class AdaptiveRenderDistanceController {
	private int cooldownTicksRemaining;
	private int lastAppliedChunks = -1;
	private int belowFloorStreakTicks;
	private int aboveHeadroomStreakTicks;

	/**
	 * @param ticks client ticks represented by this call (normally 1)
	 * @return the render distance in chunks after this evaluation, or -1 if unavailable
	 */
	public int evaluate(NitriumConfig config, double averageFps, BottleneckType bottleneck, int ticks) {
		if (!config.enableAdaptiveRenderDistance) {
			return -1;
		}

		Minecraft client = Minecraft.getInstance();
		if (client == null || client.level == null || client.options == null) {
			return -1;
		}

		OptionInstance<Integer> option = client.options.renderDistance();
		int current = option.get();
		if (lastAppliedChunks < 0) {
			lastAppliedChunks = current;
		}

		if (cooldownTicksRemaining > 0) {
			cooldownTicksRemaining = Math.max(0, cooldownTicksRemaining - ticks);
			return current;
		}

		if (averageFps <= 0.0) {
			return current; // no samples yet
		}

		int min = Math.max(2, config.adaptiveMinRenderDistanceChunks);
		int max = Math.max(min, effectiveMaxChunks(config));
		double target = Math.max(1, config.targetFps);
		double shrinkFps = target * config.renderDistanceShrinkFpsFraction;
		double growFps = target * config.renderDistanceGrowFpsFraction;

		int cooldownTicks = Math.max(1, config.renderDistanceCooldownSeconds) * 20;

		if (averageFps < shrinkFps && current > min) {
			belowFloorStreakTicks += ticks;
			aboveHeadroomStreakTicks = 0;
			if (belowFloorStreakTicks >= cooldownTicks) {
				return apply(option, current - 1, min, max, cooldownTicks, "FPS below floor");
			}
			return current;
		}

		// Only spend GPU/bandwidth on more chunks when we are not already GPU-bound.
		boolean growthAllowed = averageFps > growFps && bottleneck != BottleneckType.GPU_BOUND;
		if (growthAllowed && current < max) {
			aboveHeadroomStreakTicks += ticks;
			belowFloorStreakTicks = 0;
			if (aboveHeadroomStreakTicks >= cooldownTicks) {
				return apply(option, current + 1, min, max, cooldownTicks, "sustained headroom");
			}
			return current;
		}

		belowFloorStreakTicks = 0;
		aboveHeadroomStreakTicks = 0;
		return current;
	}

	private int apply(OptionInstance<Integer> option, int desired, int min, int max, int cooldownTicks, String reason) {
		int clamped = Math.clamp(desired, min, max);
		if (clamped != option.get()) {
			option.set(clamped);
			Nitrium.LOGGER.debug("Nitrium adaptive render distance: {} -> {} chunks ({})",
					lastAppliedChunks, clamped, reason);
		}
		lastAppliedChunks = clamped;
		belowFloorStreakTicks = 0;
		aboveHeadroomStreakTicks = 0;
		cooldownTicksRemaining = cooldownTicks;
		return clamped;
	}

	/** Trim the configured maximum on GPUs with little VRAM to keep chunk memory in budget. */
	private int effectiveMaxChunks(NitriumConfig config) {
		int configured = config.adaptiveMaxRenderDistanceChunks;
		GpuCapabilities gpu = GpuCapabilities.get();
		if (gpu == null) {
			return configured;
		}

		return switch (gpu.hardwareProfile()) {
			case POTATO -> Math.min(configured, 16);
			case MID -> Math.min(configured, 24);
			case GOD -> configured;
		};
	}

	public int lastAppliedChunks() {
		return lastAppliedChunks;
	}

	public void reset() {
		cooldownTicksRemaining = 0;
		belowFloorStreakTicks = 0;
		aboveHeadroomStreakTicks = 0;
		lastAppliedChunks = -1;
	}
}
