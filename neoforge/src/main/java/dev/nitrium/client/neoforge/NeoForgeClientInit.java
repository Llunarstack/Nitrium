package dev.nitrium.client.neoforge;

import dev.nitrium.client.NitriumClientBootstrap;
import dev.nitrium.client.platform.ClientEvents;

/**
 * Client-only NeoForge bootstrap, invoked from {@code FMLClientSetupEvent}. Installs the client
 * event bindings, then runs the common client init.
 */
public final class NeoForgeClientInit {
	private NeoForgeClientInit() {
	}

	public static void init() {
		ClientEvents.install(new NeoForgeClientEvents());
		NitriumClientBootstrap.init();
	}
}
