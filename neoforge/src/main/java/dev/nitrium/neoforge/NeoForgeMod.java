package dev.nitrium.neoforge;

import dev.nitrium.Nitrium;
import dev.nitrium.platform.ServerEvents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * NeoForge entrypoint: installs the platform event bindings and runs the common init. Client setup
 * is deferred to {@code FMLClientSetupEvent}, which only fires on the physical client, so that
 * doubles as the client-side guard.
 */
@Mod(Nitrium.MOD_ID)
public final class NeoForgeMod {
	public NeoForgeMod(IEventBus modEventBus) {
		ServerEvents.install(new NeoForgeServerEvents());
		Nitrium.init();

		modEventBus.addListener((FMLClientSetupEvent event) ->
				event.enqueueWork(dev.nitrium.client.neoforge.NeoForgeClientInit::init));
	}
}
