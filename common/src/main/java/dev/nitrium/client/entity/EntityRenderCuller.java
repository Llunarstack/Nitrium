package dev.nitrium.client.entity;

import dev.nitrium.client.profiling.BottleneckType;
import dev.nitrium.client.profiling.PerformanceMonitor;
import dev.nitrium.config.NitriumConfig;
import dev.nitrium.config.NitriumConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ExperienceOrb;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Caps entity render distance shorter than the terrain distance — a cheap, sizable win on
 * entity-dense scenes (mob farms, item piles, busy servers). Fail-open: anything it's unsure about
 * renders. Players, the camera/ridden entity, and glowing or named entities are never culled; items
 * and XP orbs cull sooner; the cap tightens while GPU/CPU-bound and keeps a near radius always
 * visible so nothing pops in next to the player.
 */
public final class EntityRenderCuller {
	private static final EntityRenderCuller INSTANCE = new EntityRenderCuller();

	private final AtomicLong tested = new AtomicLong();
	private final AtomicLong culled = new AtomicLong();

	// Frame-constant cull distances, recomputed once per frame instead of per entity. shouldRender is
	// called for every entity on the main pass and again on each shader shadow pass, so hoisting the
	// render-distance / bottleneck / Iris lookups out of the per-entity path is a real saving.
	private long cachedFrame = -1;
	private double frameNearSq;
	private double frameMaxNormalSq;
	private double frameMaxItemSq;

	private EntityRenderCuller() {
	}

	public static EntityRenderCuller get() {
		return INSTANCE;
	}

	/**
	 * @return {@code false} to skip rendering this entity, {@code true} to render it
	 */
	public boolean shouldRender(Entity entity, double camX, double camY, double camZ) {
		NitriumConfig config = NitriumConfigManager.get();
		if (!config.enableEntityDistanceCulling) {
			return true;
		}

		tested.incrementAndGet();

		if (isExempt(entity)) {
			return true;
		}

		refreshFrameCache(config);

		double dx = entity.getX() - camX;
		double dy = entity.getY() - camY;
		double dz = entity.getZ() - camZ;
		double distanceSq = dx * dx + dy * dy + dz * dz;

		if (distanceSq <= frameNearSq) {
			return true;
		}

		double maxSq = isDecorative(entity) ? frameMaxItemSq : frameMaxNormalSq;
		if (distanceSq <= maxSq) {
			return true;
		}

		culled.incrementAndGet();
		return false;
	}

	private static boolean isDecorative(Entity entity) {
		return entity instanceof ItemEntity || entity instanceof ExperienceOrb || entity instanceof ArmorStand;
	}

	private static boolean isExempt(Entity entity) {
		if (entity instanceof Player) {
			return true;
		}
		if (entity.isCurrentlyGlowing() || entity.hasCustomName()) {
			return true;
		}

		Minecraft client = Minecraft.getInstance();
		if (client.player != null && (entity == client.player.getVehicle() || entity == client.getCameraEntity())) {
			return true;
		}
		return false;
	}

	/** Recompute the frame-constant cull distances once per rendered frame. */
	private void refreshFrameCache(NitriumConfig config) {
		long frame = PerformanceMonitor.get().latestMetrics().frameIndex();
		if (frame == cachedFrame) {
			return;
		}
		cachedFrame = frame;

		double stress = 1.0;
		BottleneckType bottleneck = PerformanceMonitor.get().dominantBottleneck();
		if (bottleneck == BottleneckType.GPU_BOUND || bottleneck == BottleneckType.CPU_BOUND) {
			stress = config.entityCullStressFactor;
		}
		// With a shader pack active, entities are also drawn into the shadow map, so a shorter entity
		// distance roughly halves their contribution to the (very expensive) shadow pass.
		if (dev.nitrium.client.governor.QualityGovernor.get().isIrisGoverned()) {
			stress *= config.shaderEntityCullFactor;
		}

		// Never exceed the terrain render distance — culling entities inside loaded terrain only.
		int terrain = Integer.MAX_VALUE;
		Minecraft client = Minecraft.getInstance();
		if (client.options != null) {
			terrain = client.options.getEffectiveRenderDistance() * 16;
		}

		double near = Math.max(0, config.entityCullNearRadiusBlocks);
		double normal = Math.max(Math.min(config.maxEntityRenderDistanceBlocks, terrain) * stress, near);
		double item = Math.max(
				Math.min(Math.min(config.maxEntityRenderDistanceBlocks, config.itemEntityRenderDistanceBlocks), terrain) * stress,
				near);

		frameNearSq = near * near;
		frameMaxNormalSq = normal * normal;
		frameMaxItemSq = item * item;
	}

	public long tested() {
		return tested.get();
	}

	public long culled() {
		return culled.get();
	}

	public double cullRate() {
		long t = tested.get();
		return t == 0 ? 0.0 : (double) culled.get() / t;
	}

	/** Reset per-frame counters (called at frame start). */
	public void resetFrame() {
		tested.set(0);
		culled.set(0);
	}
}
