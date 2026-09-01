package dev.nitrium.forge;

import dev.nitrium.Nitrium;
import dev.nitrium.platform.ServerEvents;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.common.Mod;

/**
 * Forge entrypoint: installs the platform event bindings and runs the common init. Client setup is
 * deferred to {@code FMLClientSetupEvent}, which only fires on the physical client.
 */
@Mod(Nitrium.MOD_ID)
public final class ForgeMod {
	public ForgeMod(FMLJavaModLoadingContext context) {
		ServerEvents.install(new ForgeServerEvents());
		Nitrium.init();

		var modBusGroup = context.getModBusGroup();
		FMLClientSetupEvent.getBus(modBusGroup).addListener((FMLClientSetupEvent event) ->
				event.enqueueWork(dev.nitrium.client.forge.ForgeClientInit::init));
	}
}
