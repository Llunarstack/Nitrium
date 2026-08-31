package dev.nitrium.entity;

import dev.nitrium.NitriumMod;
import dev.nitrium.compat.ModCompatibility;
import dev.nitrium.compat.NitriumFeature;
import dev.nitrium.config.NitriumConfigManager;
import dev.nitrium.platform.ServerEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;

/**
 * Server-side entity optimization: spatial indexing, tiered tick throttling, enclosure dormancy.
 */
public final class EntityOptimizationEngine {
	private static EntityOptimizationEngine instance;

	private final SpatialHashGrid spatialGrid = new SpatialHashGrid();
	private final EntityTierScheduler tierScheduler = new EntityTierScheduler();
	private final EnclosureDetector enclosureDetector = new EnclosureDetector();
	private final EntityOptimizationStats stats = new EntityOptimizationStats();
	private final Map<Integer, EntityTickDecision> tickDecisions = new HashMap<>();

	private long worldTick;

	private EntityOptimizationEngine() {
	}

	public static void init() {
		if (instance != null) {
			return;
		}

		if (!NitriumConfigManager.get().enableEntityOptimization) {
			NitriumMod.LOGGER.info("Nitrium entity optimization disabled via config");
			return;
		}

		if (!ModCompatibility.isActive(NitriumFeature.ENTITY_OPTIMIZATION)) {
			NitriumMod.LOGGER.info("Nitrium entity optimization deferred — {} handles entity ticking",
					ModCompatibility.conflictingMod(NitriumFeature.ENTITY_OPTIMIZATION));
			return;
		}

		instance = new EntityOptimizationEngine();
		instance.register();
	}

	private void register() {
		ServerEvents events = ServerEvents.get();
		events.serverTickStart(server -> {
			worldTick++;
			tickDecisions.clear();
		});
		events.serverWorldTickStart(this::onWorldTickStart);
		NitriumMod.LOGGER.info("Nitrium entity optimization engine active (server)");
	}

	private void onWorldTickStart(ServerLevel level) {
		stats.reset();
		var entities = new java.util.ArrayList<Entity>();
		level.getAllEntities().forEach(entities::add);

		spatialGrid.rebuild(entities);
		enclosureDetector.prune(entities);

		for (Entity entity : entities) {
			stats.recordIndexed();
			Player nearest = findNearestPlayer(level, entity);
			boolean enclosed = enclosureDetector.isEnclosed(entity, level);
			if (enclosed) {
				stats.recordEnclosed();
			}

			EntityExecutionTier tier = tierScheduler.resolve(entity, nearest, enclosed);
			if (tier == EntityExecutionTier.TIER_3_DORMANT) {
				stats.recordDormant();
			}

			boolean shouldTick = tier != EntityExecutionTier.TIER_3_DORMANT
					&& tier.shouldRunPhysics(worldTick);
			boolean shouldRunAi = shouldTick && tier.shouldRunAi(worldTick);

			if (!shouldTick) {
				stats.recordTickSkipped();
			}
			if (shouldTick && !shouldRunAi) {
				stats.recordAiSkipped();
			}

			tickDecisions.put(entity.getId(), new EntityTickDecision(
					tier,
					shouldTick,
					shouldRunAi,
					shouldTick,
					enclosed
			));
		}
	}

	public boolean shouldTick(Entity entity) {
		if (!NitriumConfigManager.get().enableEntityOptimization) {
			return true;
		}

		EntityTickDecision decision = tickDecisions.get(entity.getId());
		return decision == null || decision.shouldTick();
	}

	public boolean shouldRunAi(Entity entity) {
		if (!NitriumConfigManager.get().enableEntityOptimization) {
			return true;
		}

		EntityTickDecision decision = tickDecisions.get(entity.getId());
		return decision == null || decision.shouldRunAi();
	}

	public EntityTickDecision decisionFor(Entity entity) {
		return tickDecisions.getOrDefault(entity.getId(), EntityTickDecision.full(EntityExecutionTier.TIER_0_FOCUS));
	}

	public SpatialHashGrid spatialGrid() {
		return spatialGrid;
	}

	public EntityOptimizationStats stats() {
		return stats;
	}

	public static EntityOptimizationEngine get() {
		return instance;
	}

	public void onWorldUnload() {
		tickDecisions.clear();
		enclosureDetector.clear();
		spatialGrid.clear();
		stats.reset();
	}

	private static Player findNearestPlayer(Level level, Entity entity) {
		if (!(level instanceof ServerLevel serverLevel)) {
			return null;
		}

		ServerPlayer nearest = null;
		double bestDistance = Double.MAX_VALUE;

		for (ServerPlayer player : serverLevel.players()) {
			double distance = entity.distanceToSqr(player);
			if (distance < bestDistance) {
				bestDistance = distance;
				nearest = player;
			}
		}

		return nearest;
	}
}
