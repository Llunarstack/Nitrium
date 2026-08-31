package dev.nitrium.client.entity;

import dev.nitrium.Nitrium;
import dev.nitrium.client.culling.CullingPipeline;
import dev.nitrium.client.nativegl.FrustumPlaneExtractor;
import dev.nitrium.nativecore.NitriumNativeLoader;
import dev.nitrium.nativecore.SimdFrustumCuller;
import dev.nitrium.config.NitriumConfigManager;
import dev.nitrium.entity.EntityOptimizationStats;
import dev.nitrium.client.platform.ClientEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix4f;

/**
 * Client-side entity render optimization: animation throttling, instanced batching, GPU skinning.
 */
public final class EntityRenderOptimizer {
	private static EntityRenderOptimizer instance;

	private final AnimationRateLimiter animationLimiter = new AnimationRateLimiter();
	private final EntityInstanceBatch instanceBatch = new EntityInstanceBatch();
	private final EntityOptimizationStats clientStats = new EntityOptimizationStats();
	private final SimdFrustumCuller simdCuller = SimdFrustumCuller.create();
	private int lastSimdVisibleMask;
	private long frameIndex;

	private EntityRenderOptimizer() {
	}

	public static void init() {
		if (instance != null) {
			return;
		}

		if (!NitriumConfigManager.get().enableEntityOptimization) {
			Nitrium.LOGGER.info("Nitrium client entity optimization disabled via config");
			return;
		}

		instance = new EntityRenderOptimizer();
		instance.register();
	}

	private void register() {
		EntityTransformBuffer.get().init(4096);
		GpuEntitySkinningCompute.get().init();

		ClientEvents events = ClientEvents.get();
		events.worldRenderBeforeEntities(this::onBeforeEntities);
		events.worldRenderAfterEntities(this::onAfterEntities);

		Nitrium.LOGGER.info("Nitrium entity render optimizer active (client)");
	}

	private void onBeforeEntities() {
		frameIndex++;
		clientStats.reset();
		instanceBatch.clear();
		EntityRenderCuller.get().resetFrame();

		Minecraft client = Minecraft.getInstance();
		ClientLevel level = client.level;
		LocalPlayer player = client.player;
		if (level == null) {
			return;
		}

		CullingPipeline culling = CullingPipeline.get();

		if (NitriumNativeLoader.isAvailable()) {
			simdCuller.clear();
			try {
				simdCuller.setFrustumPlanes(FrustumPlaneExtractor.extractPlanes());
			} catch (Exception ignored) {
				// Frustum extraction may fail before GL is ready — Java fallback inside culler
			}
		}

		for (Entity entity : level.entitiesForRendering()) {
			clientStats.recordIndexed();

			if (NitriumNativeLoader.isAvailable()) {
				var box = entity.getBoundingBox();
				simdCuller.addAabb(
						(float) box.minX, (float) box.minY, (float) box.minZ,
						(float) box.maxX, (float) box.maxY, (float) box.maxZ
				);
			}

			boolean visible = culling == null || culling.isEntityVisible(entity.getId());
			AnimationRateLimiter.AnimationMode mode = animationLimiter.modeFor(entity, player, visible, frameIndex);

			if (mode == AnimationRateLimiter.AnimationMode.CULLED) {
				clientStats.recordTickSkipped();
				continue;
			}
			if (mode == AnimationRateLimiter.AnimationMode.FROZEN) {
				clientStats.recordDormant();
			}

			if (NitriumConfigManager.get().enableGpuEntityInstancing) {
				Matrix4f transform = new Matrix4f().translation(
						(float) entity.getX(),
						(float) entity.getY(),
						(float) entity.getZ()
				);
				int animFrame = animationLimiter.shouldUpdateAnimation(mode) ? (int) (frameIndex % 120) : 0;
				instanceBatch.add(entity, transform, animFrame);
			}
		}

		if (NitriumNativeLoader.isAvailable()) {
			lastSimdVisibleMask = simdCuller.cullVisibleMask();
		}
	}

	private void onAfterEntities() {
		if (!NitriumConfigManager.get().enableGpuEntityInstancing) {
			return;
		}

		EntityTransformBuffer.get().upload(instanceBatch);
		GpuEntitySkinningCompute.get().dispatch(instanceBatch);
		// TODO: one glDrawElementsInstanced per batch key.
	}

	public static EntityRenderOptimizer get() {
		return instance;
	}

	public EntityOptimizationStats clientStats() {
		return clientStats;
	}

	public EntityInstanceBatch instanceBatch() {
		return instanceBatch;
	}

	public AnimationRateLimiter animationLimiter() {
		return animationLimiter;
	}

	public void onWorldUnload() {
		instanceBatch.clear();
		clientStats.reset();
		simdCuller.clear();
	}
}
