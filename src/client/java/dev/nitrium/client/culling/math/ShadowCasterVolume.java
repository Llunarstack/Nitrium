package dev.nitrium.client.culling.math;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Conservative volume of space from which geometry can cast shadows visible to the camera.
 * Built by extruding the camera view bounds along the negated sun direction.
 */
public final class ShadowCasterVolume {
	private final AABB bounds;

	private ShadowCasterVolume(AABB bounds) {
		this.bounds = bounds;
	}

	public static ShadowCasterVolume build(Vec3 cameraPos, Vec3 sunDirection, double viewDistance, double shadowDistance) {
		Vec3 lightDir = sunDirection.normalize();

		// Approximate the view bounds as a sphere around the camera.
		// TODO: tighten this to the actual view-frustum hull.
		double viewRadius = viewDistance;
		AABB viewBounds = new AABB(
				cameraPos.x - viewRadius,
				cameraPos.y - viewRadius,
				cameraPos.z - viewRadius,
				cameraPos.x + viewRadius,
				cameraPos.y + viewRadius,
				cameraPos.z + viewRadius
		);

		AABB extrudedMin = viewBounds.move(-lightDir.x * shadowDistance, -lightDir.y * shadowDistance, -lightDir.z * shadowDistance);
		AABB extrudedMax = viewBounds.move(lightDir.x * shadowDistance, lightDir.y * shadowDistance, lightDir.z * shadowDistance);

		AABB combined = viewBounds.minmax(extrudedMin).minmax(extrudedMax);
		return new ShadowCasterVolume(combined);
	}

	public boolean intersects(AABB candidate) {
		return bounds.intersects(candidate);
	}

	public AABB bounds() {
		return bounds;
	}
}
