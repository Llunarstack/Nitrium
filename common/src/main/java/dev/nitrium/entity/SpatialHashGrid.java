package dev.nitrium.entity;

import dev.nitrium.config.NitriumConfigManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lock-free spatial hash grid for O(1) entity bucket lookups.
 * Buckets are {@code spatialBucketSize}³ block volumes (default 4×4×4).
 */
public final class SpatialHashGrid {
	private final Map<Long, List<Entity>> buckets = new HashMap<>();
	private int bucketSize;
	private int emptyBuckets;

	public void rebuild(Iterable<Entity> entities) {
		buckets.clear();
		bucketSize = NitriumConfigManager.get().spatialBucketSize;
		emptyBuckets = 0;

		for (Entity entity : entities) {
			insert(entity);
		}
	}

	public void insert(Entity entity) {
		long key = bucketKey(entity.position());
		buckets.computeIfAbsent(key, ignored -> new ArrayList<>()).add(entity);
	}

	/**
	 * Returns entities in the same bucket and 26 adjacent buckets.
	 */
	public List<Entity> queryNearby(Vec3 position) {
		List<Entity> result = new ArrayList<>();
		int bx = bucketCoord(position.x);
		int by = bucketCoord(position.y);
		int bz = bucketCoord(position.z);

		for (int dx = -1; dx <= 1; dx++) {
			for (int dy = -1; dy <= 1; dy++) {
				for (int dz = -1; dz <= 1; dz++) {
					long key = pack(bx + dx, by + dy, bz + dz);
					List<Entity> bucket = buckets.get(key);
					if (bucket == null || bucket.isEmpty()) {
						emptyBuckets++;
						continue;
					}
					result.addAll(bucket);
				}
			}
		}

		return result;
	}

	public int bucketCount() {
		return buckets.size();
	}

	public int emptyBucketProbes() {
		return emptyBuckets;
	}

	/**
	 * Finds the nearest player using the spatial hash first, then falls back to a full scan.
	 */
	public Player findNearestPlayer(Entity entity, Iterable<? extends Player> players) {
		Vec3 position = entity.position();
		Player nearest = null;
		double bestDistance = Double.MAX_VALUE;

		for (Entity candidate : queryNearby(position)) {
			if (candidate instanceof Player player && player.isAlive()) {
				double distance = entity.distanceToSqr(player);
				if (distance < bestDistance) {
					bestDistance = distance;
					nearest = player;
				}
			}
		}

		if (nearest != null) {
			return nearest;
		}

		for (Player player : players) {
			if (!player.isAlive()) {
				continue;
			}
			double distance = entity.distanceToSqr(player);
			if (distance < bestDistance) {
				bestDistance = distance;
				nearest = player;
			}
		}

		return nearest;
	}

	public void clear() {
		buckets.clear();
		emptyBuckets = 0;
	}

	private int bucketCoord(double coordinate) {
		return Math.floorDiv((int) Math.floor(coordinate), bucketSize);
	}

	private long bucketKey(Vec3 position) {
		return pack(bucketCoord(position.x), bucketCoord(position.y), bucketCoord(position.z));
	}

	private static long pack(int x, int y, int z) {
		return ((long) x & 0x1FFFFF) | (((long) y & 0x1FFFFF) << 21) | (((long) z & 0x1FFFFF) << 42);
	}
}
