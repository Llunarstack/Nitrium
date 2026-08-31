package dev.nitrium.itempool;

import dev.nitrium.mixin.ExperienceOrbInvoker;
import dev.nitrium.mixin.ItemEntityInvoker;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Spatial merge for XP orbs and identical dropped items within a configurable radius.
 */
public final class SpatialItemMerger {
	private SpatialItemMerger() {
	}

	public static int tryMergeExperienceOrbs(ExperienceOrb orb, int radius) {
		if (orb.level().isClientSide()) {
			return 0;
		}

		ExperienceOrbInvoker invoker = (ExperienceOrbInvoker) orb;
		AABB box = orb.getBoundingBox().inflate(radius);
		List<ExperienceOrb> nearby = orb.level().getEntitiesOfClass(ExperienceOrb.class, box,
				candidate -> candidate != orb && candidate.isAlive() && invoker.nitrium$canMerge(candidate));

		int merged = 0;
		for (ExperienceOrb other : nearby) {
			if (ExperienceOrbInvoker.nitrium$canMergeValues(other, orb.getValue(), other.getValue())) {
				invoker.nitrium$merge(other);
				merged++;
			}
		}
		return merged;
	}

	public static int tryMergeItemEntities(ItemEntity item, int radius) {
		if (item.level().isClientSide()) {
			return 0;
		}

		ItemEntityInvoker invoker = (ItemEntityInvoker) item;
		AABB box = item.getBoundingBox().inflate(radius);
		List<ItemEntity> nearby = item.level().getEntitiesOfClass(ItemEntity.class, box,
				candidate -> candidate != item && candidate.isAlive()
						&& ItemEntity.areMergable(item.getItem(), candidate.getItem()));

		int merged = 0;
		for (ItemEntity other : nearby) {
			invoker.nitrium$tryToMerge(other);
			merged++;
		}
		return merged;
	}
}
