package dev.nitrium.mixin;

import dev.nitrium.compat.ModCompatibility;
import dev.nitrium.compat.NitriumFeature;
import dev.nitrium.lighting.LightingEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ThreadedLevelLightEngine;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ThreadedLevelLightEngine.class)
public abstract class ThreadedLevelLightEngineMixin {
	@Inject(method = "updateChunkStatus", at = @At("HEAD"))
	private void nitrium$batchLightTask(ChunkPos pos, CallbackInfo callbackInfo) {
		if (!ModCompatibility.isActive(NitriumFeature.LIGHTING_ENGINE)) {
			return;
		}

		LightingEngine engine = LightingEngine.get();
		if (engine != null) {
			engine.onLightUpdate(new BlockPos(pos.getMinBlockX(), 64, pos.getMinBlockZ()));
		}
	}
}
