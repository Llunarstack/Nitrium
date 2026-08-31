package dev.nitrium.client.governor;

import dev.nitrium.config.HardwareProfile;

/**
 * A resolved, absolute-valued shader configuration produced by {@link ShaderQualityController}.
 *
 * <p>These are the values an Iris bridge feeds into shader-pack option overrides
 * (shadow distance, shadow map resolution, volumetrics, bloom, SSAO, entity shadows,
 * SSR water). Everything here is already clamped to the hardware ceiling, so the bridge
 * can apply it verbatim without recomputing budgets.
 */
public record ShaderProfile(
		ShaderQualityLevel level,
		int shadowDistanceBlocks,
		int shadowMapResolution,
		boolean volumetricLighting,
		boolean bloom,
		int ambientOcclusionQuality,
		boolean entityShadows,
		int waterReflectionQuality,
		float renderScaleFloor
) {
	/**
	 * Resolve a level against a hardware ceiling into concrete values.
	 */
	public static ShaderProfile resolve(ShaderQualityLevel level, HardwareProfile hardware) {
		int shadowDistance = Math.round(hardware.maxShadowDistanceBlocks() * level.shadowDistanceFactor());
		int shadowRes = snapToPowerOfTwo(Math.round(hardware.maxShadowMapResolution() * level.shadowResolutionFactor()));

		return new ShaderProfile(
				level,
				Math.max(16, shadowDistance),
				Math.max(256, shadowRes),
				level.volumetricLighting(),
				level.bloom(),
				level.ambientOcclusionQuality(),
				level.entityShadows(),
				level.waterReflectionQuality(),
				level.renderScaleFloor()
		);
	}

	/** Snap to the nearest power of two so shadow map allocations stay driver-friendly. */
	private static int snapToPowerOfTwo(int value) {
		if (value <= 1) {
			return 1;
		}
		int lower = Integer.highestOneBit(value);
		int upper = lower << 1;
		return (value - lower) < (upper - value) ? lower : upper;
	}

	public String summary() {
		return String.format(
				"%s [shadow %dpx@%db, vol=%s, bloom=%s, ao=%d, entShadow=%s, ssr=%d]",
				level, shadowMapResolution, shadowDistanceBlocks,
				volumetricLighting, bloom, ambientOcclusionQuality, entityShadows, waterReflectionQuality
		);
	}
}
