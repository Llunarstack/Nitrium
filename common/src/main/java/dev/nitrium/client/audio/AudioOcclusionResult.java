package dev.nitrium.client.audio;

/**
 * Result of an async audio occlusion query.
 */
public record AudioOcclusionResult(float attenuation, float reverbMix, boolean occluded) {
	public static AudioOcclusionResult clear() {
		return new AudioOcclusionResult(1.0f, 0.0f, false);
	}

	public static AudioOcclusionResult blocked(float reverb) {
		return new AudioOcclusionResult(0.15f, reverb, true);
	}
}
