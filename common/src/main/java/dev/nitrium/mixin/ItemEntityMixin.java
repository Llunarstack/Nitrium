package dev.nitrium.mixin;

import dev.nitrium.compat.ModCompatibility;
import dev.nitrium.compat.NitriumFeature;
import dev.nitrium.config.NitriumConfigManager;
import dev.nitrium.itempool.ItemXpPoolingEngine;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {
	@Inject(method = "tick", at = @At("HEAD"))
	private void nitrium$mergeNearbyItems(CallbackInfo callbackInfo) {
		if (!ModCompatibility.isActive(NitriumFeature.ITEM_XP_POOLING)) {
			return;
		}

		if (!NitriumConfigManager.get().enableItemXpPooling) {
			return;
		}

		ItemXpPoolingEngine engine = ItemXpPoolingEngine.get();
		if (engine != null) {
			engine.tryMergeItem((ItemEntity) (Object) this);
		}
	}
}
