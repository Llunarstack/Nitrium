package dev.nitrium.worldgen;

import dev.nitrium.NitriumMod;
import dev.nitrium.compat.ModCompatibility;
import dev.nitrium.compat.NitriumFeature;
import dev.nitrium.config.NitriumConfigManager;

/**
 * Facade for Nitrium world generation optimizations.
 */
public final class NitriumWorldgen {
	private NitriumWorldgen() {
	}

	public static void init() {
		if (!NitriumConfigManager.get().enableWorldgenOptimization) {
			NitriumMod.LOGGER.info("Nitrium worldgen disabled via config");
			return;
		}
		if (!ModCompatibility.isActive(NitriumFeature.WORLDGEN_OPTIMIZATION)) {
			NitriumMod.LOGGER.info("Nitrium worldgen deferred — {} handles chunk generation",
					ModCompatibility.conflictingMod(NitriumFeature.WORLDGEN_OPTIMIZATION));
			return;
		}
		WorldgenOptimizationEngine.init();
	}

	public static WorldgenOptimizationEngine get() {
		return WorldgenOptimizationEngine.get();
	}
}
