package dev.nitrium.mixin;

import dev.nitrium.compat.ModCompatibility;
import dev.nitrium.compat.NitriumFeature;
import dev.nitrium.config.NitriumConfigManager;
import dev.nitrium.itempool.ItemXpPoolingEngine;
import net.minecraft.world.entity.ExperienceOrb;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ExperienceOrb.class)
public abstract class ExperienceOrbMixin {
	@Inject(method = "tick", at = @At("HEAD"))
	private void nitrium$mergeNearbyOrbs(CallbackInfo callbackInfo) {
		if (!ModCompatibility.isActive(NitriumFeature.ITEM_XP_POOLING)) {
			return;
		}

		if (!NitriumConfigManager.get().enableItemXpPooling) {
			return;
		}

		ItemXpPoolingEngine engine = ItemXpPoolingEngine.get();
		if (engine != null) {
			engine.tryMergeOrb((ExperienceOrb) (Object) this);
		}
	}
}
