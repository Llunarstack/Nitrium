package dev.nitrium.client.culling.entity;

import dev.nitrium.client.culling.CullResult;
import dev.nitrium.client.culling.terrain.HiZOcclusionCuller;
import dev.nitrium.client.culling.CullingStats;
import dev.nitrium.compat.ModCompatibility;
import dev.nitrium.compat.NitriumFeature;
import dev.nitrium.config.NitriumConfigManager;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Entity visibility from velocity-expanded bounding proxies plus GPU occlusion queries. Expanding
 * the proxy along velocity avoids pop-in on fast movers without a fail-open ray budget. The GPU
 * {@code GL_SAMPLES_PASSED} path isn't implemented yet, so right now this only frustum-tests.
 */
public final class EntityOcclusionCuller {
	private final EntityVelocityTracker velocityTracker = new EntityVelocityTracker();

	public EntityVelocityTracker velocityTracker() {
		return velocityTracker;
	}

	public CullResult evaluate(Entity entity, Frustum viewFrustum, CullingStats stats) {
		stats.recordEntityTested();

		if (!NitriumConfigManager.get().enableEntityOcclusion
				|| !ModCompatibility.isActive(NitriumFeature.GPU_ENTITY_OCCLUSION)) {
			return CullResult.VISIBLE;
		}

		AABB bounds = expandedBounds(entity);
		if (!viewFrustum.isVisible(bounds)) {
			stats.recordEntityOccluded();
			return CullResult.OCCLUDED;
		}

		if (HiZOcclusionCuller.get().isPyramidReady()
				&& GpuEntityOcclusionQuery.get().isOccludedByHiZ(bounds)) {
			stats.recordEntityOccluded();
			return CullResult.OCCLUDED;
		}

		return CullResult.VISIBLE;
	}

	public AABB expandedBounds(Entity entity) {
		velocityTracker.update(entity);
		AABB base = entity.getBoundingBox();
		var config = NitriumConfigManager.get();
		EntityVelocityTracker.MotionSample motion = velocityTracker.get(entity.getId());

		if (motion == null || motion.speedSquared() < 1.0E-6) {
			return base;
		}

		double scale = config.entityVelocityLookaheadTicks;
		Vec3 expansion = new Vec3(
				motion.velocityX() * scale,
				motion.velocityY() * scale,
				motion.velocityZ() * scale
		);

		AABB expanded = base;
		if (expansion.x > 0) {
			expanded = expanded.expandTowards(expansion.x, 0.0, 0.0);
		} else if (expansion.x < 0) {
			expanded = new AABB(expanded.minX + expansion.x, expanded.minY, expanded.minZ, expanded.maxX, expanded.maxY, expanded.maxZ);
		}

		if (expansion.y > 0) {
			expanded = expanded.expandTowards(0.0, expansion.y, 0.0);
		} else if (expansion.y < 0) {
			expanded = new AABB(expanded.minX, expanded.minY + expansion.y, expanded.minZ, expanded.maxX, expanded.maxY, expanded.maxZ);
		}

		if (expansion.z > 0) {
			expanded = expanded.expandTowards(0.0, 0.0, expansion.z);
		} else if (expansion.z < 0) {
			expanded = new AABB(expanded.minX, expanded.minY, expanded.minZ + expansion.z, expanded.maxX, expanded.maxY, expanded.maxZ);
		}

		return expanded;
	}
}
