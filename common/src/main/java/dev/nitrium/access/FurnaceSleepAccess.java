package dev.nitrium.access;

import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;

/**
 * Mixin duck-type accessor for furnace sleep-state detection.
 *
 * <p>Lives outside the {@code dev.nitrium.mixin} package because Mixin forbids
 * referencing non-mixin classes that reside inside a configured mixin package.
 */
public interface FurnaceSleepAccess {
	boolean nitrium$isIdle();

	static boolean isIdle(AbstractFurnaceBlockEntity furnace) {
		return ((FurnaceSleepAccess) furnace).nitrium$isIdle();
	}
}
