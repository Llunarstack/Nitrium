package dev.nitrium.client.entity;

import dev.nitrium.config.NitriumConfigManager;
import dev.nitrium.entity.EntityExecutionTier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * Distance-aware animation update rate for client-side entity rendering.
 */
public final class AnimationRateLimiter {
	public enum AnimationMode {
		FULL,
		THROTTLED,
		FROZEN,
		CULLED
	}

	public AnimationMode modeFor(Entity entity, Player player, boolean visible, long frameIndex) {
		if (!NitriumConfigManager.get().enableAnimationThrottling) {
			return AnimationMode.FULL;
		}

		if (!visible) {
			return AnimationMode.CULLED;
		}

		if (player == null) {
			return AnimationMode.THROTTLED;
		}

		double distanceSq = entity.distanceToSqr(player);
		int near = NitriumConfigManager.get().tier0DistanceBlocks;
		int mid = NitriumConfigManager.get().tier1DistanceBlocks;

		if (distanceSq <= (long) near * near) {
			return AnimationMode.FULL;
		}
		if (distanceSq <= (long) mid * mid) {
			// ~15 FPS animation at 60 FPS render
			return frameIndex % 4 == 0 ? AnimationMode.THROTTLED : AnimationMode.FROZEN;
		}

		return AnimationMode.FROZEN;
	}

	public boolean shouldUpdateAnimation(AnimationMode mode) {
		return mode == AnimationMode.FULL || mode == AnimationMode.THROTTLED;
	}

	public EntityExecutionTier tierHint(AnimationMode mode) {
		return switch (mode) {
			case FULL -> EntityExecutionTier.TIER_0_FOCUS;
			case THROTTLED -> EntityExecutionTier.TIER_1_NEAR;
			case FROZEN -> EntityExecutionTier.TIER_2_FAR;
			case CULLED -> EntityExecutionTier.TIER_3_DORMANT;
		};
	}
}
