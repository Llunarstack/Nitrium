package dev.nitrium.client.culling.shadow;

import dev.nitrium.client.culling.CullResult;
import dev.nitrium.client.culling.math.ShadowCasterVolume;
import dev.nitrium.config.NitriumConfigManager;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Drops shadow-pass geometry that cannot cast shadows visible to the active camera.
 * Computes the intersection volume between the view frustum and sun light extrusion.
 */
public final class LightFrustumShadowCuller {
	private ShadowCasterVolume activeVolume;

	public void update(Vec3 cameraPos, Vec3 sunDirection, double viewDistance) {
		var config = NitriumConfigManager.get();
		activeVolume = ShadowCasterVolume.build(
				cameraPos,
				sunDirection,
				viewDistance,
				config.shadowCullDistanceBlocks
		);
	}

	/**
	 * @return {@link CullResult#SHADOW_CULLED} when the section should skip Iris shadow passes.
	 */
	public CullResult evaluateSection(Frustum viewFrustum, AABB sectionBounds) {
		if (!NitriumConfigManager.get().enableShadowFrustumCulling || activeVolume == null) {
			return CullResult.VISIBLE;
		}

		boolean inView = viewFrustum.isVisible(sectionBounds);
		if (inView) {
			return CullResult.VISIBLE;
		}

		// Outside view frustum — only cast shadows if inside the light-view intersection volume.
		if (!activeVolume.intersects(sectionBounds)) {
			return CullResult.SHADOW_CULLED;
		}

		return CullResult.VISIBLE;
	}
}
