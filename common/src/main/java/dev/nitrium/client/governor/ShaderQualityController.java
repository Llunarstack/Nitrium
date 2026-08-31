package dev.nitrium.client.governor;

import dev.nitrium.Nitrium;
import dev.nitrium.client.profiling.BottleneckType;
import dev.nitrium.client.profiling.GpuCapabilities;
import dev.nitrium.config.HardwareProfile;
import dev.nitrium.config.NitriumConfig;

/**
 * Walks the {@link ShaderQualityLevel} ladder with hysteresis: a sustained FPS deficit steps
 * quality down, sustained headroom steps it up, and every change arms a cooldown so shader
 * recompiles don't thrash. Upgrades are skipped while CPU-bound (heavier shaders only cost GPU).
 * The result is {@link #currentProfile()}; decisions are clamped to the hardware ceiling so a weak
 * GPU never reaches {@link ShaderQualityLevel#CINEMATIC}.
 */
public final class ShaderQualityController {
	private ShaderQualityLevel currentLevel = ShaderQualityLevel.BALANCED;
	private ShaderQualityLevel hardwareCeiling = ShaderQualityLevel.CINEMATIC;
	private ShaderProfile currentProfile;

	private int downgradeStreakSeconds;
	private int upgradeStreakSeconds;
	private int cooldownSecondsRemaining;
	private int lastTransitionTick;

	public ShaderQualityController() {
		this.currentProfile = ShaderProfile.resolve(currentLevel, HardwareProfile.MID);
	}

	/**
	 * Evaluate one slow-loop step.
	 *
	 * @param intervalSeconds seconds represented by this evaluation
	 * @param averageFps      rolling average FPS
	 * @param bottleneck      dominant bottleneck for the window
	 * @return {@code true} if the quality level changed
	 */
	public boolean evaluate(NitriumConfig config, int intervalSeconds, double averageFps, BottleneckType bottleneck) {
		refreshHardwareCeiling();

		if (cooldownSecondsRemaining > 0) {
			cooldownSecondsRemaining = Math.max(0, cooldownSecondsRemaining - intervalSeconds);
			return false;
		}

		double target = Math.max(1, config.targetFps);
		double downgradeFps = target * config.shaderDowngradeFpsFraction;
		double upgradeFps = target * config.shaderUpgradeFpsFraction;

		if (averageFps <= 0.0) {
			return false; // no data yet
		}

		if (averageFps < downgradeFps) {
			downgradeStreakSeconds += intervalSeconds;
			upgradeStreakSeconds = 0;
		} else if (averageFps > upgradeFps && bottleneck != BottleneckType.CPU_BOUND) {
			upgradeStreakSeconds += intervalSeconds;
			downgradeStreakSeconds = 0;
		} else {
			downgradeStreakSeconds = 0;
			upgradeStreakSeconds = 0;
			return false;
		}

		if (downgradeStreakSeconds >= config.shaderDowngradeStabilitySeconds && currentLevel.ordinal() > 0) {
			return transition(config, currentLevel.stepDown(), "FPS deficit");
		}

		if (upgradeStreakSeconds >= config.shaderUpgradeStabilitySeconds
				&& currentLevel.isCheaperThan(hardwareCeiling)) {
			return transition(config, currentLevel.stepUp(hardwareCeiling), "sustained headroom");
		}

		return false;
	}

	private boolean transition(NitriumConfig config, ShaderQualityLevel next, String reason) {
		if (next == currentLevel) {
			return false;
		}

		ShaderQualityLevel previous = currentLevel;
		currentLevel = next;
		currentProfile = ShaderProfile.resolve(currentLevel, hardwareProfile());
		downgradeStreakSeconds = 0;
		upgradeStreakSeconds = 0;
		cooldownSecondsRemaining = config.shaderTransitionCooldownSeconds;

		Nitrium.LOGGER.info("Nitrium shader governor: {} -> {} ({}) | {}",
				previous, currentLevel, reason, currentProfile.summary());
		return true;
	}

	private void refreshHardwareCeiling() {
		HardwareProfile profile = hardwareProfile();
		ShaderQualityLevel ceiling = switch (profile) {
			case POTATO -> ShaderQualityLevel.PERFORMANCE;
			case MID -> ShaderQualityLevel.HIGH;
			case GOD -> ShaderQualityLevel.CINEMATIC;
		};

		if (ceiling != hardwareCeiling) {
			hardwareCeiling = ceiling;
			// A newly detected weaker ceiling must pull the current level down immediately.
			if (currentLevel.ordinal() > hardwareCeiling.ordinal()) {
				currentLevel = hardwareCeiling;
				currentProfile = ShaderProfile.resolve(currentLevel, profile);
			}
		}
	}

	private HardwareProfile hardwareProfile() {
		GpuCapabilities gpu = GpuCapabilities.get();
		return gpu != null ? gpu.hardwareProfile() : HardwareProfile.MID;
	}

	public ShaderQualityLevel currentLevel() {
		return currentLevel;
	}

	public ShaderQualityLevel hardwareCeiling() {
		return hardwareCeiling;
	}

	public ShaderProfile currentProfile() {
		return currentProfile;
	}

	public int cooldownSecondsRemaining() {
		return cooldownSecondsRemaining;
	}

	public void reset() {
		currentLevel = ShaderQualityLevel.BALANCED;
		downgradeStreakSeconds = 0;
		upgradeStreakSeconds = 0;
		cooldownSecondsRemaining = 0;
		currentProfile = ShaderProfile.resolve(currentLevel, hardwareProfile());
	}
}
