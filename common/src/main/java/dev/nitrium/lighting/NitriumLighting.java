package dev.nitrium.lighting;

import dev.nitrium.Nitrium;
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
			Nitrium.LOGGER.info("Nitrium lighting engine disabled via config");
			return;
		}
		if (!ModCompatibility.isActive(NitriumFeature.LIGHTING_ENGINE)) {
			Nitrium.LOGGER.info("Nitrium lighting deferred — {} handles light propagation",
					ModCompatibility.conflictingMod(NitriumFeature.LIGHTING_ENGINE));
			return;
		}
		LightingEngine.init();
	}

	public static LightingEngine get() {
		return LightingEngine.get();
	}
}
