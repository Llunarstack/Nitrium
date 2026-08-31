package dev.nitrium.mixin;

import dev.nitrium.compat.ModCompatibility;
import dev.nitrium.compat.NitriumFeature;
import dev.nitrium.entity.EntityOptimizationEngine;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityTickThrottleMixin {
	@Inject(method = "tick", at = @At("HEAD"), cancellable = true)
	private void nitrium$throttleEntityTick(CallbackInfo callbackInfo) {
		if (!ModCompatibility.isActive(NitriumFeature.ENTITY_OPTIMIZATION)) {
			return;
		}

		EntityOptimizationEngine engine = EntityOptimizationEngine.get();
		if (engine == null) {
			return;
		}

		Entity self = (Entity) (Object) this;
		if (!engine.shouldTick(self)) {
			callbackInfo.cancel();
		}
	}
}
