package dev.nitrium;

import dev.nitrium.fabric.FabricServerEvents;
import dev.nitrium.platform.ServerEvents;
import net.fabricmc.api.ModInitializer;

public final class NitriumMod implements ModInitializer {
	@Override
	public void onInitialize() {
		ServerEvents.install(new FabricServerEvents());
		Nitrium.init();
	}
}
