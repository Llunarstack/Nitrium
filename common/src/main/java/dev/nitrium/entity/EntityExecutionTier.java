package dev.nitrium.entity;

/**
 * Distance-based execution tiers for entity AI, physics, and animation.
 */
public enum EntityExecutionTier {
	/** Player look-target or &lt;16 blocks — full rate. */
	TIER_0_FOCUS(1, 1),
	/** 16–48 blocks — reduced AI, full movement. */
	TIER_1_NEAR(4, 1),
	/** 48–96 blocks — minimal AI, interpolated physics. */
	TIER_2_FAR(20, 4),
	/** &gt;96 blocks or enclosed — frozen AI and physics. */
	TIER_3_DORMANT(Integer.MAX_VALUE, Integer.MAX_VALUE);

	private final int aiTickInterval;
	private final int physicsTickInterval;

	EntityExecutionTier(int aiTickInterval, int physicsTickInterval) {
		this.aiTickInterval = aiTickInterval;
		this.physicsTickInterval = physicsTickInterval;
	}

	public int aiTickInterval() {
		return aiTickInterval;
	}

	public int physicsTickInterval() {
		return physicsTickInterval;
	}

	public boolean shouldRunAi(long worldTick) {
		return aiTickInterval == 1 || worldTick % aiTickInterval == 0L;
	}

	public boolean shouldRunPhysics(long worldTick) {
		return physicsTickInterval == 1 || worldTick % physicsTickInterval == 0L;
	}
}
