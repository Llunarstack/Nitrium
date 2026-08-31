package dev.nitrium.client.render.oit;

import dev.nitrium.NitriumMod;
import dev.nitrium.config.NitriumConfigManager;

/**
 * Facade for weighted blended order-independent transparency.
 */
public final class NitriumOit {
	private NitriumOit() {
	}

	public static void init() {
		if (!NitriumConfigManager.get().enableOitTranslucency) {
			NitriumMod.LOGGER.info("Nitrium OIT translucency disabled via config");
			return;
		}
		WeightedBlendedOitPipeline.init();
	}

	public static WeightedBlendedOitPipeline get() {
		return WeightedBlendedOitPipeline.get();
	}
}
