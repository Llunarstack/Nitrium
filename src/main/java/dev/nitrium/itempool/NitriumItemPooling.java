package dev.nitrium.itempool;

import dev.nitrium.NitriumMod;
import dev.nitrium.compat.ModCompatibility;
import dev.nitrium.compat.NitriumFeature;
import dev.nitrium.config.NitriumConfigManager;

/**
 * Facade for XP orb and dropped-item spatial pooling.
 */
public final class NitriumItemPooling {
	private NitriumItemPooling() {
	}

	public static void init() {
		if (!NitriumConfigManager.get().enableItemXpPooling) {
			NitriumMod.LOGGER.info("Nitrium item/XP pooling disabled via config");
			return;
		}
		if (!ModCompatibility.isActive(NitriumFeature.ITEM_XP_POOLING)) {
			NitriumMod.LOGGER.info("Nitrium item/XP pooling deferred — {} handles orb merging",
					ModCompatibility.conflictingMod(NitriumFeature.ITEM_XP_POOLING));
			return;
		}
		ItemXpPoolingEngine.init();
	}

	public static ItemXpPoolingEngine get() {
		return ItemXpPoolingEngine.get();
	}
}
