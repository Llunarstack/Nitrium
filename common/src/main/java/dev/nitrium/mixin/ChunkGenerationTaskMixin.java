package dev.nitrium.mixin;

import dev.nitrium.compat.ModCompatibility;
import dev.nitrium.compat.NitriumFeature;
import dev.nitrium.worldgen.WorldgenOptimizationEngine;
import net.minecraft.server.level.ChunkGenerationTask;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hooks chunk-generation scheduling so the work can be routed through Nitrium's lock-free task
 * graph. The eventual goal is to replace vanilla NoiseChunk sampling with
 * {@link dev.nitrium.worldgen.noise.CoarseDensityPipeline}; for now it only observes scheduling.
 */
@Mixin(ChunkGenerationTask.class)
public abstract class ChunkGenerationTaskMixin {
	@Shadow
	@Final
	private ChunkPos pos;

	@Inject(method = "scheduleLayer", at = @At("HEAD"))
	private void nitrium$onScheduleLayer(CallbackInfo ci) {
		if (!ModCompatibility.isActive(NitriumFeature.WORLDGEN_OPTIMIZATION)) {
			return;
		}

		WorldgenOptimizationEngine engine = WorldgenOptimizationEngine.get();
		if (engine == null) {
			return;
		}

		engine.onChunkGenerationScheduled(pos.toLong());
	}
}
