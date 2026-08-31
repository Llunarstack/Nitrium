package dev.nitrium.lighting;

import dev.nitrium.NitriumMod;
import dev.nitrium.compat.ModCompatibility;
import dev.nitrium.compat.NitriumFeature;
import dev.nitrium.config.NitriumConfigManager;

/**
 * Facade for Nitrium's async bitpacked lighting engine.
 */
public final class NitriumLighting {
	private NitriumLighting() {
	}

	public static void init() {
		if (!NitriumConfigManager.get().enableLightingEngine) {
			NitriumMod.LOGGER.info("Nitrium lighting engine disabled via config");
			return;
		}
		if (!ModCompatibility.isActive(NitriumFeature.LIGHTING_ENGINE)) {
			NitriumMod.LOGGER.info("Nitrium lighting deferred — {} handles light propagation",
					ModCompatibility.conflictingMod(NitriumFeature.LIGHTING_ENGINE));
			return;
		}
		LightingEngine.init();
	}

	public static LightingEngine get() {
		return LightingEngine.get();
	}
}
