package dev.nitrium.storage;

import dev.nitrium.NitriumMod;
import dev.nitrium.config.NitriumConfigManager;

/**
 * Facade for async ring-buffer chunk storage.
 */
public final class NitriumStorage {
	private NitriumStorage() {
	}

	public static void init() {
		if (!NitriumConfigManager.get().enableAsyncChunkStorage) {
			NitriumMod.LOGGER.info("Nitrium async chunk storage disabled via config");
			return;
		}
		AsyncChunkStorageEngine.init();
	}

	public static AsyncChunkStorageEngine get() {
		return AsyncChunkStorageEngine.get();
	}
}
