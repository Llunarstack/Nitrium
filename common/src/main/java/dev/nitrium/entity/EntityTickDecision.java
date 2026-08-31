package dev.nitrium.entity;

import net.minecraft.world.entity.Entity;

/**
 * Cached per-entity tick decision for the current game tick.
 */
public record EntityTickDecision(
		EntityExecutionTier tier,
		boolean shouldTick,
		boolean shouldRunAi,
		boolean shouldRunPhysics,
		boolean enclosed
) {
	public static EntityTickDecision full(EntityExecutionTier tier) {
		return new EntityTickDecision(tier, true, true, true, false);
	}

	public static EntityTickDecision skip(EntityExecutionTier tier, boolean enclosed) {
		return new EntityTickDecision(tier, false, false, false, enclosed);
	}
}
