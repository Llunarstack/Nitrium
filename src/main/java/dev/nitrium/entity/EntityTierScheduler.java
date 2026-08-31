package dev.nitrium.entity;

import dev.nitrium.config.NitriumConfig;
import dev.nitrium.config.NitriumConfigManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Assigns entities to distance-based execution tiers relative to the nearest player.
 */
public final class EntityTierScheduler {
	public EntityExecutionTier resolve(Entity entity, Player nearestPlayer, boolean enclosed) {
		if (enclosed) {
			return EntityExecutionTier.TIER_3_DORMANT;
		}

		if (nearestPlayer == null) {
			return EntityExecutionTier.TIER_2_FAR;
		}

		NitriumConfig config = NitriumConfigManager.get();
		double distanceSq = entity.distanceToSqr(nearestPlayer);

		if (isLookTarget(entity, nearestPlayer) || distanceSq <= sq(config.tier0DistanceBlocks)) {
			return EntityExecutionTier.TIER_0_FOCUS;
		}
		if (distanceSq <= sq(config.tier1DistanceBlocks)) {
			return EntityExecutionTier.TIER_1_NEAR;
		}
		if (distanceSq <= sq(config.tier2DistanceBlocks)) {
			return EntityExecutionTier.TIER_2_FAR;
		}

		return EntityExecutionTier.TIER_3_DORMANT;
	}

	private static boolean isLookTarget(Entity entity, Player player) {
		Entity lookedAt = player.getRootVehicle();
		if (lookedAt == entity) {
			return true;
		}

		Vec3 look = player.getViewVector(1.0F);
		Vec3 toEntity = entity.position().subtract(player.position()).normalize();
		return look.dot(toEntity) > 0.96;
	}

	private static double sq(int blocks) {
		return (double) blocks * blocks;
	}
}
