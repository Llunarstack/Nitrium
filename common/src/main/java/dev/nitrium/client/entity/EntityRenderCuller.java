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

		double dx = entity.getX() - camX;
		double dy = entity.getY() - camY;
		double dz = entity.getZ() - camZ;
		double distanceSq = dx * dx + dy * dy + dz * dz;

		double near = Math.max(0, config.entityCullNearRadiusBlocks);
		if (distanceSq <= near * near) {
			return true;
		}

		double maxBlocks = effectiveMaxDistance(entity, config);
		if (distanceSq <= maxBlocks * maxBlocks) {
			return true;
		}

		culled.incrementAndGet();
		return false;
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

	private double effectiveMaxDistance(Entity entity, NitriumConfig config) {
		int cap = config.maxEntityRenderDistanceBlocks;

		// Cheap, high-count decorative entities are culled sooner.
		if (entity instanceof ItemEntity || entity instanceof ExperienceOrb || entity instanceof ArmorStand) {
			cap = Math.min(cap, config.itemEntityRenderDistanceBlocks);
		}

		// Never exceed the terrain render distance — culling entities inside loaded terrain only.
		Minecraft client = Minecraft.getInstance();
		if (client.options != null) {
			cap = Math.min(cap, client.options.getEffectiveRenderDistance() * 16);
		}

		double distance = cap;
		BottleneckType bottleneck = PerformanceMonitor.get().dominantBottleneck();
		if (bottleneck == BottleneckType.GPU_BOUND || bottleneck == BottleneckType.CPU_BOUND) {
			distance *= config.entityCullStressFactor;
		}

		// With a shader pack active, entities are also drawn into the shadow map, so a shorter
		// entity distance roughly halves their contribution to the (very expensive) shadow pass.
		if (dev.nitrium.client.governor.QualityGovernor.get().isIrisGoverned()) {
			distance *= config.shaderEntityCullFactor;
		}

		// Keep the cap at or above the near radius so the near guarantee always holds.
		return Math.max(distance, config.entityCullNearRadiusBlocks);
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
