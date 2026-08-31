package dev.nitrium.client.culling;

/**
 * Result of a single culling stage for a render candidate.
 */
public enum CullResult {
	/** Fully visible — submit to draw pipeline. */
	VISIBLE,
	/** Hidden by Hi-Z or frustum — skip geometry pass. */
	OCCLUDED,
	/** Visible in main pass but excluded from shadow map rendering. */
	SHADOW_CULLED,
	/** Culled from main pass; may still cast shadows (rare). */
	MAIN_CULLED
}
