package dev.nitrium.entity;

import dev.nitrium.config.NitriumConfigManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

/**
 * Detects fully enclosed stationary entities (villager cells, animal pens) for dormancy.
 */
public final class EnclosureDetector {
	private final Map<Integer, IdleState> idleStates = new HashMap<>();

	public boolean isEnclosed(Entity entity, Level level) {
		int id = entity.getId();
		IdleState state = idleStates.computeIfAbsent(id, ignored -> new IdleState());

		Vec3 pos = entity.position();
		if (state.lastX != null && pos.distanceToSqr(state.lastX, state.lastY, state.lastZ) > 1.0E-4) {
			state.stationaryTicks = 0;
		}

		state.lastX = pos.x;
		state.lastY = pos.y;
		state.lastZ = pos.z;
		state.stationaryTicks++;

		int threshold = NitriumConfigManager.get().enclosureIdleTicks;
		if (state.stationaryTicks < threshold) {
			return false;
		}

		return isSpatiallyEnclosed(entity, level);
	}

	public void prune(Iterable<Entity> activeEntities) {
		var active = new java.util.HashSet<Integer>();
		for (Entity entity : activeEntities) {
			active.add(entity.getId());
		}
		idleStates.keySet().removeIf(id -> !active.contains(id));
	}

	public void clear() {
		idleStates.clear();
	}

	private static boolean isSpatiallyEnclosed(Entity entity, Level level) {
		AABB box = entity.getBoundingBox();
		BlockPos min = BlockPos.containing(box.minX, box.minY, box.minZ);
		BlockPos max = BlockPos.containing(box.maxX, box.maxY, box.maxZ);

		for (int x = min.getX() - 1; x <= max.getX() + 1; x++) {
			for (int y = min.getY() - 1; y <= max.getY() + 1; y++) {
				for (int z = min.getZ() - 1; z <= max.getZ() + 1; z++) {
					boolean inside = x >= min.getX() && x <= max.getX()
							&& y >= min.getY() && y <= max.getY()
							&& z >= min.getZ() && z <= max.getZ();
					if (inside) {
						continue;
					}

					BlockState block = level.getBlockState(new BlockPos(x, y, z));
					if (!block.isSolidRender()) {
						return false;
					}
				}
			}
		}

		return true;
	}

	private static final class IdleState {
		private Double lastX;
		private Double lastY;
		private Double lastZ;
		private int stationaryTicks;
	}
}
