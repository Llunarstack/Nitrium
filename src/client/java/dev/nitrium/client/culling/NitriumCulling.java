package dev.nitrium.client.culling;

import dev.nitrium.NitriumMod;
import dev.nitrium.compat.ModCompatibility;
import dev.nitrium.compat.NitriumFeature;
import dev.nitrium.config.NitriumConfigManager;

/**
 * Entry point for Nitrium's GPU-aware culling pipeline.
 */
public final class NitriumCulling {
	private NitriumCulling() {
	}

	public static void init() {
		if (!NitriumConfigManager.get().enableCullingPipeline) {
			NitriumMod.LOGGER.info("Nitrium culling disabled via config");
			return;
		}

		CullingPipeline.init();

		if (!ModCompatibility.isActive(NitriumFeature.GPU_ENTITY_OCCLUSION)) {
			NitriumMod.LOGGER.info("Nitrium entity occlusion deferred — {} is active",
					ModCompatibility.conflictingMod(NitriumFeature.GPU_ENTITY_OCCLUSION));
		} else if (ModCompatibility.isSodiumLoaded()) {
			NitriumMod.LOGGER.info("Nitrium culling complements Sodium (Hi-Z + shadow frustum active)");
		}
	}
}
