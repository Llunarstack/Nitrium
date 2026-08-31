package dev.nitrium.layout;

import dev.nitrium.Nitrium;
import dev.nitrium.config.NitriumConfigManager;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages SoA section layouts and block-state bitfield palettes.
 */
public final class MemoryLayoutEngine {
	private static MemoryLayoutEngine instance;

	private final Map<Long, SectionSoALayout> sections = new HashMap<>();
	private final RegistryDeduplicator deduplicator = new RegistryDeduplicator();
	private final LayoutStats stats = new LayoutStats();

	private MemoryLayoutEngine() {
	}

	public static void init() {
		if (instance != null) {
			return;
		}
		instance = new MemoryLayoutEngine();
		Nitrium.LOGGER.info("Nitrium memory layout engine active (bitfields={})",
				NitriumConfigManager.get().enableBlockStateBitfields);
	}

	public SectionSoALayout section(long key) {
		return sections.computeIfAbsent(key, ignored -> {
			stats.recordSectionAllocated();
			return new SectionSoALayout();
		});
	}

	public int packBlockState(int paletteId, int facing, int flags) {
		if (!NitriumConfigManager.get().enableBlockStateBitfields) {
			return paletteId;
		}
		return BlockStateBitfieldPalette.pack(paletteId, facing, flags);
	}

	public RegistryDeduplicator deduplicator() {
		return deduplicator;
	}

	public LayoutStats stats() {
		return stats;
	}

	public static MemoryLayoutEngine get() {
		return instance;
	}

	public void onWorldUnload() {
		sections.clear();
		deduplicator.clear();
		stats.reset();
	}
}
