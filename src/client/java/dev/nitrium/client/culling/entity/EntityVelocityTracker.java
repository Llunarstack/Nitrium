package dev.nitrium.client.culling.entity;

import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.Map;

/**
 * Tracks per-entity velocity for temporal proxy bounding-box expansion.
 */
public final class EntityVelocityTracker {
	private final Map<Integer, MotionSample> samples = new HashMap<>();

	public void update(Entity entity) {
		int id = entity.getId();
		MotionSample previous = samples.get(id);

		double x = entity.getX();
		double y = entity.getY();
		double z = entity.getZ();

		if (previous == null) {
			samples.put(id, new MotionSample(x, y, z, 0.0, 0.0, 0.0));
			return;
		}

		samples.put(id, new MotionSample(
				x,
				y,
				z,
				x - previous.x,
				y - previous.y,
				z - previous.z
		));
	}

	public MotionSample get(int entityId) {
		return samples.get(entityId);
	}

	public void prune(Iterable<Entity> activeEntities) {
		var activeIds = new java.util.HashSet<Integer>();
		for (Entity entity : activeEntities) {
			activeIds.add(entity.getId());
		}
		samples.keySet().removeIf(id -> !activeIds.contains(id));
	}

	public void clear() {
		samples.clear();
	}

	public record MotionSample(double x, double y, double z, double velocityX, double velocityY, double velocityZ) {
		public double speedSquared() {
			return velocityX * velocityX + velocityY * velocityY + velocityZ * velocityZ;
		}
	}
}
