package dev.nitrium.mixin;

import dev.nitrium.compat.ModCompatibility;
import dev.nitrium.compat.NitriumFeature;
import dev.nitrium.entity.EntityOptimizationEngine;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityAiThrottleMixin {
	@Inject(method = "aiStep", at = @At("HEAD"), cancellable = true)
	private void nitrium$throttleAiStep(CallbackInfo callbackInfo) {
		if (!ModCompatibility.isActive(NitriumFeature.ENTITY_OPTIMIZATION)) {
			return;
		}

		EntityOptimizationEngine engine = EntityOptimizationEngine.get();
		if (engine == null) {
			return;
		}

		LivingEntity self = (LivingEntity) (Object) this;
		if (!engine.shouldRunAi(self)) {
			callbackInfo.cancel();
		}
	}
}
