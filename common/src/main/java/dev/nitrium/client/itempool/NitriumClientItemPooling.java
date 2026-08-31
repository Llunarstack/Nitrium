package dev.nitrium.client.itempool;

import dev.nitrium.Nitrium;
import dev.nitrium.config.NitriumConfigManager;

/**
 * Client side of item/XP pooling: far-away dropped items get drawn as cheap billboards instead of
 * full entities. The billboard renderer itself is still a stub.
 */
public final class NitriumClientItemPooling {
	private static NitriumClientItemPooling instance;

	private final DistantItemBillboardRenderer billboards = new DistantItemBillboardRenderer();

	private NitriumClientItemPooling() {
	}

	public static void init() {
		if (!NitriumConfigManager.get().enableItemXpPooling) {
			return;
		}
		if (instance != null) {
			return;
		}
		instance = new NitriumClientItemPooling();
		Nitrium.LOGGER.info("Nitrium client item billboards active (distance={} blocks)",
				NitriumConfigManager.get().distantItemBillboardBlocks);
	}

	public DistantItemBillboardRenderer billboards() {
		return billboards;
	}

	public static NitriumClientItemPooling get() {
		return instance;
	}

	public void onWorldUnload() {
		billboards.clear();
	}
}
