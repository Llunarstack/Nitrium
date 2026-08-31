package dev.nitrium.client.gui;

import dev.nitrium.NitriumMod;
import dev.nitrium.config.NitriumConfigManager;

/**
 * Facade for SDF font rendering and cached HUD layers.
 */
public final class NitriumGui {
	private NitriumGui() {
	}

	public static void init() {
		if (!NitriumConfigManager.get().enableGuiSdfCache) {
			NitriumMod.LOGGER.info("Nitrium GUI SDF cache disabled via config");
			return;
		}
		GuiRenderEngine.init();
	}

	public static GuiRenderEngine get() {
		return GuiRenderEngine.get();
	}
}
