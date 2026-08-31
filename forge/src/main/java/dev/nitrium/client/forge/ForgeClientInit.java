package dev.nitrium.client.forge;

import dev.nitrium.client.NitriumClientBootstrap;
import dev.nitrium.client.platform.ClientEvents;

/**
 * Client-only Forge bootstrap, invoked from {@code FMLClientSetupEvent}.
 */
public final class ForgeClientInit {
	private ForgeClientInit() {
	}

	public static void init() {
		ClientEvents.install(new ForgeClientEvents());
		NitriumClientBootstrap.init();
	}
}
