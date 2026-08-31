package dev.nitrium.itempool;

import dev.nitrium.Nitrium;
import dev.nitrium.config.NitriumConfigManager;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;

/**
 * Server-side XP orb and item entity pooling engine.
 */
public final class ItemXpPoolingEngine {
	private static ItemXpPoolingEngine instance;

	private final ItemPoolingStats stats = new ItemPoolingStats();

	private ItemXpPoolingEngine() {
	}

	public static void init() {
		if (instance != null) {
			return;
		}
		instance = new ItemXpPoolingEngine();
		Nitrium.LOGGER.info("Nitrium item/XP pooling active (radius={} blocks)",
				NitriumConfigManager.get().itemMergeRadiusBlocks);
	}

	public void tryMergeOrb(ExperienceOrb orb) {
		int radius = NitriumConfigManager.get().itemMergeRadiusBlocks;
		int merged = SpatialItemMerger.tryMergeExperienceOrbs(orb, radius);
		if (merged > 0) {
			stats.recordOrbMerge(merged);
		}
	}

	public void tryMergeItem(ItemEntity item) {
		int radius = NitriumConfigManager.get().itemMergeRadiusBlocks;
		int merged = SpatialItemMerger.tryMergeItemEntities(item, radius);
		if (merged > 0) {
			stats.recordItemMerge(merged);
		}
	}

	public ItemPoolingStats stats() {
		return stats;
	}

	public static ItemXpPoolingEngine get() {
		return instance;
	}

	public void onWorldUnload() {
		stats.reset();
	}
}
