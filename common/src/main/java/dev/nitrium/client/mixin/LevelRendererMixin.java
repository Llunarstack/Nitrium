package dev.nitrium.client.mixin;

import dev.nitrium.client.platform.ClientRenderStages;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fires {@link ClientRenderStages} at the world-render boundaries. The handlers capture none of the
 * target method's arguments (they only fire a no-arg callback), so they stay valid even as Mojang
 * changes {@code renderLevel}'s parameter list between versions.
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
	@Inject(method = "renderLevel", at = @At("HEAD"), require = 0)
	private void nitrium$renderStart(CallbackInfo ci) {
		ClientRenderStages.fireRenderStart();
	}

	@Inject(method = "renderLevel", at = @At("RETURN"), require = 0)
	private void nitrium$renderEnd(CallbackInfo ci) {
		ClientRenderStages.fireRenderEnd();
	}

	@Inject(method = "extractVisibleEntities", at = @At("RETURN"), require = 0)
	private void nitrium$beforeEntities(CallbackInfo ci) {
		ClientRenderStages.fireBeforeEntities();
	}

	@Inject(
			method = "renderLevel",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/renderer/debug/DebugRenderer;emitGizmos(Lnet/minecraft/client/renderer/culling/Frustum;DDDF)V",
					shift = At.Shift.AFTER
			),
			require = 0
	)
	private void nitrium$beforeDebug(CallbackInfo ci) {
		ClientRenderStages.fireBeforeDebug();
	}

	@Inject(method = "submitEntities", at = @At("RETURN"), require = 0)
	private void nitrium$afterEntities(CallbackInfo ci) {
		ClientRenderStages.fireAfterEntities();
	}
}
