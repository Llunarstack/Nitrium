package dev.nitrium.layout;

import dev.nitrium.Nitrium;
import dev.nitrium.compat.ModCompatibility;
import dev.nitrium.compat.NitriumFeature;
import dev.nitrium.config.NitriumConfigManager;

/**
 * Facade for SoA memory layout and block-state bitfield compression.
 */
public final class NitriumMemoryLayout {
	private NitriumMemoryLayout() {
	}

	public static void init() {
		if (!NitriumConfigManager.get().enableMemoryLayoutOptimization) {
			Nitrium.LOGGER.info("Nitrium memory layout optimization disabled via config");
			return;
		}
		if (!ModCompatibility.isActive(NitriumFeature.MEMORY_LAYOUT)) {
			Nitrium.LOGGER.info("Nitrium memory layout deferred — {} handles heap compression",
					ModCompatibility.conflictingMod(NitriumFeature.MEMORY_LAYOUT));
			return;
		}
		MemoryLayoutEngine.init();
	}

	public static MemoryLayoutEngine get() {
		return MemoryLayoutEngine.get();
	}
}
