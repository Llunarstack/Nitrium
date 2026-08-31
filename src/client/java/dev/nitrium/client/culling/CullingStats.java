package dev.nitrium.client.culling;

/**
 * Per-frame counters for the Nitrium culling pipeline.
 */
public final class CullingStats {
	private int sectionsTested;
	private int sectionsOccludedHiZ;
	private int sectionsShadowCulled;
	private int entitiesTested;
	private int entitiesOccluded;
	private int entitiesVelocityExpanded;
	private int foliageSectionsOptimized;

	public void reset() {
		sectionsTested = 0;
		sectionsOccludedHiZ = 0;
		sectionsShadowCulled = 0;
		entitiesTested = 0;
		entitiesOccluded = 0;
		entitiesVelocityExpanded = 0;
		foliageSectionsOptimized = 0;
	}

	public void recordSectionTested() {
		sectionsTested++;
	}

	public void recordSectionOccludedHiZ() {
		sectionsOccludedHiZ++;
	}

	public void recordSectionShadowCulled() {
		sectionsShadowCulled++;
	}

	public void recordEntityTested() {
		entitiesTested++;
	}

	public void recordEntityOccluded() {
		entitiesOccluded++;
	}

	public void recordEntityVelocityExpanded() {
		entitiesVelocityExpanded++;
	}

	public void recordFoliageOptimized() {
		foliageSectionsOptimized++;
	}

	public int sectionsTested() {
		return sectionsTested;
	}

	public int sectionsOccludedHiZ() {
		return sectionsOccludedHiZ;
	}

	public int sectionsShadowCulled() {
		return sectionsShadowCulled;
	}

	public int entitiesTested() {
		return entitiesTested;
	}

	public int entitiesOccluded() {
		return entitiesOccluded;
	}

	public int entitiesVelocityExpanded() {
		return entitiesVelocityExpanded;
	}

	public int foliageSectionsOptimized() {
		return foliageSectionsOptimized;
	}

	public float sectionHiZCullRate() {
		return sectionsTested == 0 ? 0.0f : (float) sectionsOccludedHiZ / sectionsTested;
	}

	public float shadowCullRate() {
		return sectionsTested == 0 ? 0.0f : (float) sectionsShadowCulled / sectionsTested;
	}

	public float entityCullRate() {
		return entitiesTested == 0 ? 0.0f : (float) entitiesOccluded / entitiesTested;
	}
}
