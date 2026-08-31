package dev.nitrium.client.network;

import dev.nitrium.NitriumMod;
import dev.nitrium.config.NitriumConfigManager;
import dev.nitrium.network.PacketPipeline;

/**
 * Client-side network pipeline facade.
 */
public final class NitriumClientNetwork {
	private NitriumClientNetwork() {
	}

	public static void init() {
		if (!NitriumConfigManager.get().enableNetworkPipeline) {
			return;
		}
		NitriumMod.LOGGER.info("Nitrium client network pipeline active");
	}

	public static PacketPipeline pipeline() {
		return PacketPipeline.get();
	}
}
