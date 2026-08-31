package dev.nitrium.client.streaming;

import dev.nitrium.Nitrium;
import dev.nitrium.compat.ModCompatibility;
import dev.nitrium.compat.NitriumFeature;
import dev.nitrium.config.NitriumConfigManager;

/**
 * Entry point for Nitrium's extreme render-distance streaming pipeline.
 */
public final class NitriumStreaming {
	private NitriumStreaming() {
	}

	public static void init() {
		if (!NitriumConfigManager.get().enableSectionDiskCache) {
			Nitrium.LOGGER.info("Nitrium streaming disabled via config");
			return;
		}
		if (!ModCompatibility.isActive(NitriumFeature.SECTION_DISK_CACHE)) {
			Nitrium.LOGGER.info("Nitrium section disk cache deferred — {} handles chunk caching",
					ModCompatibility.conflictingMod(NitriumFeature.SECTION_DISK_CACHE));
			return;
		}

		AsyncChunkCacheStore.init();
		StreamingChunkLoader.init();
		GeometryBufferPool.init();
	}
}
