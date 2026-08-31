package dev.nitrium.client.culling;

import dev.nitrium.NitriumMod;
import dev.nitrium.client.culling.entity.EntityOcclusionCuller;
import dev.nitrium.client.culling.entity.GpuEntityOcclusionQuery;
import dev.nitrium.client.culling.foliage.FoliageCullPolicy;
import dev.nitrium.client.culling.terrain.HiZOcclusionCuller;
import dev.nitrium.client.culling.terrain.SectionCullEvaluator;
import dev.nitrium.client.streaming.SectionKey;
import dev.nitrium.config.NitriumConfig;
import dev.nitrium.config.NitriumConfigManager;
import dev.nitrium.client.platform.ClientEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central culling orchestrator: Hi-Z terrain, light-frustum shadows, entity proxies, foliage policy.
 */
public final class CullingPipeline {
	private static CullingPipeline instance;

	private final CullingStats stats = new CullingStats();
	private final SectionCullEvaluator sectionCuller = new SectionCullEvaluator();
	private final EntityOcclusionCuller entityCuller = new EntityOcclusionCuller();
	private final FoliageCullPolicy foliagePolicy = new FoliageCullPolicy();

	/** Sections approved for draw submission this frame. */
	private final Set<SectionKey> visibleSections = ConcurrentHashMap.newKeySet();

	/** Entity IDs approved for rendering this frame. */
	private final Set<Integer> visibleEntities = ConcurrentHashMap.newKeySet();

	private CullingPipeline() {
	}

	public static void init() {
		if (instance != null) {
			return;
		}

		instance = new CullingPipeline();
		instance.register();
	}

	private void register() {
		NitriumConfig config = NitriumConfigManager.get();
		if (!config.enableCullingPipeline) {
			NitriumMod.LOGGER.info("Nitrium culling pipeline disabled via config");
			return;
		}

		ClientEvents events = ClientEvents.get();
		events.worldRenderStart(this::onWorldRenderStart);
		events.worldRenderBeforeEntities(() -> {
			Minecraft client = Minecraft.getInstance();
			Vec3 cameraPos = client.gameRenderer.getMainCamera().position();
			onBeforeEntities(cameraPos);
		});
		events.worldRenderEnd(this::onWorldRenderEnd);

		events.clientTickEnd(this::onClientTickEnd);

		NitriumMod.LOGGER.info("Nitrium culling pipeline active");
	}

	private void onWorldRenderStart() {
		stats.reset();
		visibleSections.clear();
		visibleEntities.clear();

		Minecraft client = Minecraft.getInstance();
		if (client.getWindow() != null) {
			HiZOcclusionCuller.get().probeCapabilities();
			GpuEntityOcclusionQuery.get().init(NitriumConfigManager.get().entityProxyBufferSize);
			GpuEntityOcclusionQuery.get().beginFrame();
		}
	}

	private void onBeforeEntities(Vec3 cameraPos) {
		Minecraft client = Minecraft.getInstance();
		ClientLevel level = client.level;
		if (level == null) {
			return;
		}

		Frustum frustum = captureFrustum(client);
		if (frustum == null) {
			return;
		}

		Vec3 sunDirection = estimateSunDirection(client);

		double viewDistance = client.options.getEffectiveRenderDistance() * 16.0;
		sectionCuller.shadowCuller().update(cameraPos, sunDirection, viewDistance);

		for (Entity entity : level.entitiesForRendering()) {
			CullResult result = entityCuller.evaluate(entity, frustum, stats);
			if (result == CullResult.VISIBLE) {
				visibleEntities.add(entity.getId());
			}
		}

		entityCuller.velocityTracker().prune(level.entitiesForRendering());
	}

	private void onWorldRenderEnd() {
		Minecraft client = Minecraft.getInstance();
		if (client.getWindow() != null) {
			HiZOcclusionCuller.get().buildDepthPyramid(
					client.getWindow().getWidth(),
					client.getWindow().getHeight()
			);
			GpuEntityOcclusionQuery.get().endFrame();
		}
	}

	private void onClientTickEnd(Minecraft client) {
		// Sample the foliage policy for nearby sections. TODO: hook this into the mesh builder.
		ClientLevel level = client.level;
		if (level == null || client.player == null) {
			return;
		}

		Vec3 camera = client.player.position();
		BlockPos center = client.player.blockPosition();
		int radius = 4;

		for (int dx = -radius; dx <= radius; dx++) {
			for (int dy = -1; dy <= 1; dy++) {
				for (int dz = -radius; dz <= radius; dz++) {
					BlockPos pos = center.offset(dx, dy, dz);
					BlockState state = level.getBlockState(pos);
					if (foliagePolicy.shouldOptimize(state, camera, pos)) {
						stats.recordFoliageOptimized();
					}
				}
			}
		}
	}

	/**
	 * Evaluate a streaming/cached section for visibility. Called by the streaming loader.
	 */
	public boolean submitSection(SectionKey key, Frustum frustum) {
		CullResult result = sectionCuller.evaluate(key, frustum, stats);
		if (result == CullResult.VISIBLE || result == CullResult.SHADOW_CULLED) {
			visibleSections.add(key);
			return result == CullResult.VISIBLE;
		}
		return false;
	}

	public boolean isEntityVisible(int entityId) {
		return visibleEntities.isEmpty() || visibleEntities.contains(entityId);
	}

	public boolean isSectionVisible(SectionKey key) {
		return visibleSections.contains(key);
	}

	public CullingStats stats() {
		return stats;
	}

	public FoliageCullPolicy foliagePolicy() {
		return foliagePolicy;
	}

	public static CullingPipeline get() {
		return instance;
	}

	public void onWorldUnload() {
		visibleSections.clear();
		visibleEntities.clear();
		stats.reset();
		entityCuller.velocityTracker().clear();
	}

	private static Frustum captureFrustum(Minecraft client) {
		try {
			var field = client.levelRenderer.getClass().getDeclaredField("cullingFrustum");
			field.setAccessible(true);
			return (Frustum) field.get(client.levelRenderer);
		} catch (ReflectiveOperationException exception) {
			// TODO: replace this reflection with an accessor mixin on LevelRenderer.
			return null;
		}
	}

	private static Vec3 estimateSunDirection(Minecraft client) {
		if (client.level == null) {
			return new Vec3(0.0, -1.0, 0.0);
		}

		long dayTime = client.level.getDayTime();
		float sunAngle = (float) (dayTime % 24000L) / 24000.0F * ((float) Math.PI * 2.0F);
		return new Vec3(-Math.sin(sunAngle), Math.cos(sunAngle), 0.0).normalize();
	}
}
