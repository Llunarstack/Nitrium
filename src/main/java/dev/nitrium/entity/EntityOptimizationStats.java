package dev.nitrium.entity;

/**
 * Per-frame entity optimization counters.
 */
public final class EntityOptimizationStats {
	private int entitiesIndexed;
	private int ticksSkipped;
	private int aiTicksSkipped;
	private int dormantEntities;
	private int enclosedEntities;
	private int collisionBucketsSkipped;

	public void reset() {
		entitiesIndexed = 0;
		ticksSkipped = 0;
		aiTicksSkipped = 0;
		dormantEntities = 0;
		enclosedEntities = 0;
		collisionBucketsSkipped = 0;
	}

	public void recordIndexed() {
		entitiesIndexed++;
	}

	public void recordTickSkipped() {
		ticksSkipped++;
	}

	public void recordAiSkipped() {
		aiTicksSkipped++;
	}

	public void recordDormant() {
		dormantEntities++;
	}

	public void recordEnclosed() {
		enclosedEntities++;
	}

	public void recordCollisionBucketSkipped() {
		collisionBucketsSkipped++;
	}

	public int entitiesIndexed() {
		return entitiesIndexed;
	}

	public int ticksSkipped() {
		return ticksSkipped;
	}

	public int aiTicksSkipped() {
		return aiTicksSkipped;
	}

	public int dormantEntities() {
		return dormantEntities;
	}

	public int enclosedEntities() {
		return enclosedEntities;
	}

	public int collisionBucketsSkipped() {
		return collisionBucketsSkipped;
	}

	public float tickSkipRate() {
		return entitiesIndexed == 0 ? 0.0f : (float) ticksSkipped / entitiesIndexed;
	}
}
