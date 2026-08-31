package dev.nitrium.mixin;

import dev.nitrium.compat.ModCompatibility;
import dev.nitrium.compat.NitriumFeature;
import dev.nitrium.config.NitriumConfigManager;
import dev.nitrium.redstone.RedstoneOptimizationEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HopperBlockEntity.class)
public abstract class HopperBlockEntityMixin {
	@Inject(method = "pushItemsTick", at = @At("HEAD"), cancellable = true)
	private static void nitrium$skipSleepingHopper(
			Level level,
			BlockPos pos,
			BlockState state,
			HopperBlockEntity hopper,
			CallbackInfo callbackInfo
	) {
		if (!ModCompatibility.isActive(NitriumFeature.BLOCK_ENTITY_SLEEP)) {
			return;
		}

		RedstoneOptimizationEngine engine = RedstoneOptimizationEngine.get();
		if (engine == null || !NitriumConfigManager.get().enableBlockEntitySleep) {
			return;
		}

		if (engine.sleepRegistry().shouldSkipHopperTick(hopper)) {
			engine.stats().recordHopperTickSkipped();
			callbackInfo.cancel();
		}
	}

	@Inject(method = "entityInside", at = @At("HEAD"))
	private static void nitrium$wakeHopper(
			Level level,
			BlockPos pos,
			BlockState state,
			Entity entity,
			HopperBlockEntity hopper,
			CallbackInfo callbackInfo
	) {
		if (!ModCompatibility.isActive(NitriumFeature.BLOCK_ENTITY_SLEEP)) {
			return;
		}

		RedstoneOptimizationEngine engine = RedstoneOptimizationEngine.get();
		if (engine != null) {
			engine.sleepRegistry().wakeHopper(pos);
		}
	}
}
