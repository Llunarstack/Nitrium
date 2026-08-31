package dev.nitrium.redstone;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;

import java.util.HashSet;
import java.util.Set;

/**
 * Event-driven sleep registry for dormant tile entities (hoppers, furnaces).
 */
public final class BlockEntitySleepRegistry {
	private final Set<Long> sleepingHoppers = new HashSet<>();
	private final Set<Long> sleepingFurnaces = new HashSet<>();

	public boolean shouldSkipHopperTick(HopperBlockEntity hopper) {
		long key = posKey(hopper.getBlockPos());
		if (!sleepingHoppers.contains(key)) {
			if (isHopperIdle(hopper)) {
				sleepingHoppers.add(key);
				return true;
			}
			return false;
		}
		return true;
	}

	public boolean shouldSkipFurnaceTick(BlockEntity furnace) {
		long key = posKey(furnace.getBlockPos());
		if (!sleepingFurnaces.contains(key)) {
			if (isFurnaceIdle(furnace)) {
				sleepingFurnaces.add(key);
				return true;
			}
			return false;
		}
		return true;
	}

	public void wakeHopper(BlockPos pos) {
		sleepingHoppers.remove(posKey(pos));
	}

	public void wakeFurnace(BlockPos pos) {
		sleepingFurnaces.remove(posKey(pos));
	}

	public void clear() {
		sleepingHoppers.clear();
		sleepingFurnaces.clear();
	}

	public int sleepingHopperCount() {
		return sleepingHoppers.size();
	}

	public int sleepingFurnaceCount() {
		return sleepingFurnaces.size();
	}

	private static boolean isHopperIdle(HopperBlockEntity hopper) {
		return hopper.isEmpty();
	}

	private static boolean isFurnaceIdle(BlockEntity furnace) {
		// TODO: check the furnace's fuel/cook progress via an accessor instead of assuming idle.
		return true;
	}

	private static long posKey(BlockPos pos) {
		return pos.asLong();
	}
}
