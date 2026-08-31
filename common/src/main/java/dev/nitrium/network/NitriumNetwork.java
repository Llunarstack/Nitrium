package dev.nitrium.network;

import dev.nitrium.NitriumMod;
import dev.nitrium.compat.ModCompatibility;
import dev.nitrium.compat.NitriumFeature;
import dev.nitrium.config.NitriumConfigManager;

/**
 * Facade for off-thread packet compression pipeline.
 */
public final class NitriumNetwork {
	private NitriumNetwork() {
	}

	public static void init() {
		if (!NitriumConfigManager.get().enableNetworkPipeline) {
			NitriumMod.LOGGER.info("Nitrium network pipeline disabled via config");
			return;
		}
		if (!ModCompatibility.isActive(NitriumFeature.NETWORK_PIPELINE)) {
			NitriumMod.LOGGER.info("Nitrium network pipeline deferred — {} handles packet compression",
					ModCompatibility.conflictingMod(NitriumFeature.NETWORK_PIPELINE));
			return;
		}
		PacketPipeline.init();
	}

	public static PacketPipeline get() {
		return PacketPipeline.get();
	}
}
