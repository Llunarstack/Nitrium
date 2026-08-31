package dev.nitrium.redstone;

import dev.nitrium.NitriumMod;
import dev.nitrium.compat.ModCompatibility;
import dev.nitrium.compat.NitriumFeature;
import dev.nitrium.config.NitriumConfigManager;

/**
 * Facade for topological redstone and block-entity sleep optimization.
 */
public final class NitriumRedstone {
	private NitriumRedstone() {
	}

	public static void init() {
		if (!NitriumConfigManager.get().enableRedstoneOptimization) {
			NitriumMod.LOGGER.info("Nitrium redstone optimization disabled via config");
			return;
		}
		if (!ModCompatibility.isActive(NitriumFeature.BLOCK_ENTITY_SLEEP)
				&& !ModCompatibility.isActive(NitriumFeature.REDSTONE_TOPOLOGICAL)) {
			NitriumMod.LOGGER.info("Nitrium redstone deferred — external mod handles hoppers/redstone");
			return;
		}
		RedstoneOptimizationEngine.init();
	}

	public static RedstoneOptimizationEngine get() {
		return RedstoneOptimizationEngine.get();
	}
}
