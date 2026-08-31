package dev.nitrium.mixin;

import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ItemEntity.class)
public interface ItemEntityInvoker {
	@Invoker("tryToMerge")
	void nitrium$tryToMerge(ItemEntity other);
}
