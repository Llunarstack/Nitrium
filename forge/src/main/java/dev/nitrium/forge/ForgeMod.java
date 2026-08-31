package dev.nitrium.forge;

import dev.nitrium.Nitrium;
import dev.nitrium.platform.ServerEvents;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Forge entrypoint: installs the platform event bindings and runs the common init. Client setup is
 * deferred to {@code FMLClientSetupEvent}, which only fires on the physical client.
 */
@Mod(Nitrium.MOD_ID)
public final class ForgeMod {
	public ForgeMod(IEventBus modEventBus) {
		ServerEvents.install(new ForgeServerEvents());
		Nitrium.init();

		modEventBus.addListener((FMLClientSetupEvent event) ->
				event.enqueueWork(dev.nitrium.client.forge.ForgeClientInit::init));
	}
}
