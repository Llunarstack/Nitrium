package dev.nitrium.client;

import dev.nitrium.client.fabric.FabricClientEvents;
import dev.nitrium.client.platform.ClientEvents;
import net.fabricmc.api.ClientModInitializer;

public final class NitriumClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientEvents.install(new FabricClientEvents());
		NitriumClientBootstrap.init();
	}
}
