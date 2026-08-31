package dev.nitrium.client.governor;

/**
 * Structural shader quality ladder. Each rung expresses relative cost knobs that the
 * {@link ShaderQualityController} resolves against the detected hardware ceiling into a
 * concrete {@link ShaderProfile}. Ordered cheapest ({@link #SURVIVAL}) to most expensive
 * ({@link #CINEMATIC}); {@link #ordinal()} is the rung index used for up/down stepping.
 */
public enum ShaderQualityLevel {
	SURVIVAL(0.35f, 0.5f, false, false, 0, false, 0, 0.60f),
	PERFORMANCE(0.55f, 0.5f, false, true, 1, false, 0, 0.75f),
	BALANCED(0.75f, 1.0f, true, true, 1, true, 1, 0.85f),
	HIGH(0.90f, 1.0f, true, true, 2, true, 2, 1.00f),
	CINEMATIC(1.00f, 1.0f, true, true, 2, true, 2, 1.00f);

	/** Fraction of the hardware shadow-distance ceiling to request. */
	private final float shadowDistanceFactor;
	/** Fraction of the hardware shadow-map resolution ceiling to request. */
	private final float shadowResolutionFactor;
	private final boolean volumetricLighting;
	private final boolean bloom;
	/** 0 = off, 1 = low, 2 = high. */
	private final int ambientOcclusionQuality;
	private final boolean entityShadows;
	/** 0 = none, 1 = screen-space low, 2 = screen-space high. */
	private final int waterReflectionQuality;
	/** Lower bound the dynamic-resolution controller may scale to at this rung. */
	private final float renderScaleFloor;

	ShaderQualityLevel(
			float shadowDistanceFactor,
			float shadowResolutionFactor,
			boolean volumetricLighting,
			boolean bloom,
			int ambientOcclusionQuality,
			boolean entityShadows,
			int waterReflectionQuality,
			float renderScaleFloor
	) {
		this.shadowDistanceFactor = shadowDistanceFactor;
		this.shadowResolutionFactor = shadowResolutionFactor;
		this.volumetricLighting = volumetricLighting;
		this.bloom = bloom;
		this.ambientOcclusionQuality = ambientOcclusionQuality;
		this.entityShadows = entityShadows;
		this.waterReflectionQuality = waterReflectionQuality;
		this.renderScaleFloor = renderScaleFloor;
	}

	public float shadowDistanceFactor() {
		return shadowDistanceFactor;
	}

	public float shadowResolutionFactor() {
		return shadowResolutionFactor;
	}

	public boolean volumetricLighting() {
		return volumetricLighting;
	}

	public boolean bloom() {
		return bloom;
	}

	public int ambientOcclusionQuality() {
		return ambientOcclusionQuality;
	}

	public boolean entityShadows() {
		return entityShadows;
	}

	public int waterReflectionQuality() {
		return waterReflectionQuality;
	}

	public float renderScaleFloor() {
		return renderScaleFloor;
	}

	public ShaderQualityLevel stepDown() {
		return ordinal() == 0 ? this : values()[ordinal() - 1];
	}

	public ShaderQualityLevel stepUp(ShaderQualityLevel ceiling) {
		int next = Math.min(ordinal() + 1, ceiling.ordinal());
		return values()[next];
	}

	public boolean isCheaperThan(ShaderQualityLevel other) {
		return ordinal() < other.ordinal();
	}
}
