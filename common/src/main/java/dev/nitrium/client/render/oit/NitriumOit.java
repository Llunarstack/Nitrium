package dev.nitrium.client.render.oit;

import dev.nitrium.Nitrium;
import dev.nitrium.config.NitriumConfigManager;

/**
 * Facade for weighted blended order-independent transparency.
 */
public final class NitriumOit {
	private NitriumOit() {
	}

	public static void init() {
		if (!NitriumConfigManager.get().enableOitTranslucency) {
			Nitrium.LOGGER.info("Nitrium OIT translucency disabled via config");
			return;
		}
		WeightedBlendedOitPipeline.init();
	}

	public static WeightedBlendedOitPipeline get() {
		return WeightedBlendedOitPipeline.get();
	}
}
