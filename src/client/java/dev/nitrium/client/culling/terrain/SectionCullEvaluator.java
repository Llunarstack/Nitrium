package dev.nitrium.client.culling.terrain;

import dev.nitrium.client.culling.CullResult;
import dev.nitrium.client.culling.CullingStats;
import dev.nitrium.client.culling.shadow.LightFrustumShadowCuller;
import dev.nitrium.client.streaming.SectionKey;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.AABB;

/**
 * Evaluates terrain section visibility through the Hi-Z and shadow-frustum pipeline.
 */
public final class SectionCullEvaluator {
	private final HiZOcclusionCuller hiZ = HiZOcclusionCuller.get();
	private final LightFrustumShadowCuller shadowCuller = new LightFrustumShadowCuller();

	public LightFrustumShadowCuller shadowCuller() {
		return shadowCuller;
	}

	public CullResult evaluate(SectionKey key, Frustum viewFrustum, CullingStats stats) {
		stats.recordSectionTested();
		AABB bounds = sectionBounds(key);

		if (!viewFrustum.isVisible(bounds)) {
			CullResult shadow = shadowCuller.evaluateSection(viewFrustum, bounds);
			if (shadow == CullResult.SHADOW_CULLED) {
				stats.recordSectionShadowCulled();
			}
			return CullResult.OCCLUDED;
		}

		if (hiZ.isOccluded(bounds)) {
			stats.recordSectionOccludedHiZ();
			return CullResult.OCCLUDED;
		}

		CullResult shadow = shadowCuller.evaluateSection(viewFrustum, bounds);
		if (shadow == CullResult.SHADOW_CULLED) {
			stats.recordSectionShadowCulled();
		}

		return shadow;
	}

	public static AABB sectionBounds(SectionKey key) {
		double minX = key.blockX();
		double minY = key.blockY();
		double minZ = key.blockZ();
		return new AABB(minX, minY, minZ, minX + 16.0, minY + 16.0, minZ + 16.0);
	}
}
