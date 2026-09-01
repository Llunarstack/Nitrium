package dev.nitrium.storage;

import dev.nitrium.Nitrium;
import dev.nitrium.config.NitriumConfigManager;
import net.minecraft.world.level.chunk.storage.RegionFileVersion;

import java.util.Locale;

/**
 * Facade for chunk storage: region-file compression selection plus the (optional) async saver.
 */
public final class NitriumStorage {
	private NitriumStorage() {
	}

	public static void init() {
		applyRegionCompression();

		if (!NitriumConfigManager.get().enableAsyncChunkStorage) {
			return;
		}
		AsyncChunkStorageEngine.init();
	}

	/**
	 * Switch the region-file write codec (LZ4/none/deflate) via Minecraft's own selector. Reads stay
	 * per-chunk, so this never breaks existing worlds. Applies to worlds this client saves; dedicated
	 * servers set {@code region-file-compression} in server.properties, so we don't override that.
	 */
	private static void applyRegionCompression() {
		String choice = NitriumConfigManager.get().regionFileCompression;
		if (choice == null) {
			return;
		}
		choice = choice.trim().toLowerCase(Locale.ROOT);
		if (choice.isEmpty() || choice.equals("default")) {
			return;
		}
		if (!choice.equals("lz4") && !choice.equals("none") && !choice.equals("deflate")) {
			Nitrium.LOGGER.warn("Nitrium: unknown regionFileCompression '{}' — leaving Minecraft's default", choice);
			return;
		}

		RegionFileVersion.configure(choice);
		Nitrium.LOGGER.info("Nitrium region-file write compression set to '{}'", choice);
	}

	public static AsyncChunkStorageEngine get() {
		return AsyncChunkStorageEngine.get();
	}
}
