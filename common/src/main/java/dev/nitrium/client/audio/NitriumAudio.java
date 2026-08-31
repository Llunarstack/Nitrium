package dev.nitrium.client.audio;

import dev.nitrium.NitriumMod;
import dev.nitrium.config.NitriumConfigManager;

/**
 * Facade for async spatial audio occlusion.
 */
public final class NitriumAudio {
	private NitriumAudio() {
	}

	public static void init() {
		if (!NitriumConfigManager.get().enableAsyncAudioOcclusion) {
			NitriumMod.LOGGER.info("Nitrium async audio occlusion disabled via config");
			return;
		}
		AsyncAudioOcclusionEngine.init();
	}

	public static AsyncAudioOcclusionEngine get() {
		return AsyncAudioOcclusionEngine.get();
	}
}
