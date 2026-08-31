package dev.nitrium.mixin;

import dev.nitrium.access.FurnaceSleepAccess;
import dev.nitrium.compat.ModCompatibility;
import dev.nitrium.compat.NitriumFeature;
import dev.nitrium.config.NitriumConfigManager;
import dev.nitrium.redstone.RedstoneOptimizationEngine;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class AbstractFurnaceBlockEntityMixin implements FurnaceSleepAccess {
	@Shadow
	private int litTimeRemaining;

	@Shadow
	private int cookingTimer;

	@Inject(method = "serverTick", at = @At("HEAD"), cancellable = true)
	private static void nitrium$skipIdleFurnace(
			ServerLevel level,
			BlockPos pos,
			BlockState state,
			AbstractFurnaceBlockEntity furnace,
			CallbackInfo callbackInfo
	) {
		if (!ModCompatibility.isActive(NitriumFeature.BLOCK_ENTITY_SLEEP)) {
			return;
		}

		RedstoneOptimizationEngine engine = RedstoneOptimizationEngine.get();
		if (engine == null || !NitriumConfigManager.get().enableBlockEntitySleep) {
			return;
		}

		if (FurnaceSleepAccess.isIdle(furnace)) {
			engine.stats().recordFurnaceTickSkipped();
			callbackInfo.cancel();
		}
	}

	@Override
	public boolean nitrium$isIdle() {
		AbstractFurnaceBlockEntity self = (AbstractFurnaceBlockEntity) (Object) this;
		return self.isEmpty() && litTimeRemaining == 0 && cookingTimer == 0;
	}
}
