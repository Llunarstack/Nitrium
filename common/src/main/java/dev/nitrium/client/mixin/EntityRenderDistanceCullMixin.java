package dev.nitrium.client.mixin;

import dev.nitrium.client.entity.EntityRenderCuller;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Skips rendering entities the {@link EntityRenderCuller} distance-culls. Injected at the head of
 * {@link EntityRenderDispatcher#shouldRender} so a {@code false} return elides all downstream
 * render-state extraction and draw submission for that entity.
 *
 * <p>{@code require = 0}: if the target signature ever shifts across a Minecraft update the
 * injector simply does not apply and rendering falls back to vanilla, rather than crashing.
 */
@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDistanceCullMixin {
	@Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true, require = 0)
	private <E extends Entity> void nitrium$distanceCull(
			E entity,
			Frustum frustum,
			double camX,
			double camY,
			double camZ,
			CallbackInfoReturnable<Boolean> cir
	) {
		if (!EntityRenderCuller.get().shouldRender(entity, camX, camY, camZ)) {
			cir.setReturnValue(false);
		}
	}
}
