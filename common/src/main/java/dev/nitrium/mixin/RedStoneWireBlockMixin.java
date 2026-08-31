package dev.nitrium.mixin;

import dev.nitrium.compat.ModCompatibility;
import dev.nitrium.compat.NitriumFeature;
import dev.nitrium.config.NitriumConfigManager;
import dev.nitrium.redstone.RedstoneOptimizationEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RedStoneWireBlock.class)
public abstract class RedStoneWireBlockMixin {
	@Inject(method = "updatePowerStrength", at = @At("HEAD"))
	private void nitrium$trackWireUpdate(
			Level level,
			BlockPos pos,
			BlockState state,
			Orientation orientation,
			boolean updateNeighbors,
			CallbackInfo callbackInfo
	) {
		if (!ModCompatibility.isActive(NitriumFeature.REDSTONE_TOPOLOGICAL)) {
			return;
		}

		if (!NitriumConfigManager.get().enableTopologicalRedstone) {
			return;
		}

		RedstoneOptimizationEngine engine = RedstoneOptimizationEngine.get();
		if (engine != null) {
			engine.onWireUpdate(level, pos);
		}
	}
}
