package dev.nitrium.client.mixin;

import dev.nitrium.client.platform.ClientRenderStages;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.LevelRenderState;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
	@Inject(method = "renderLevel", at = @At("HEAD"), require = 0)
	private void nitrium$renderStart(
			DeltaTracker deltaTracker,
			boolean bl,
			Camera camera,
			Matrix4f matrix4f,
			Matrix4f matrix4f2,
			Matrix4f matrix4f3,
			com.mojang.blaze3d.buffers.GpuBufferSlice gpuBufferSlice,
			org.joml.Vector4f vector4f,
			boolean bl2,
			CallbackInfo ci
	) {
		ClientRenderStages.fireRenderStart();
	}

	@Inject(method = "renderLevel", at = @At("RETURN"), require = 0)
	private void nitrium$renderEnd(
			DeltaTracker deltaTracker,
			boolean bl,
			Camera camera,
			Matrix4f matrix4f,
			Matrix4f matrix4f2,
			Matrix4f matrix4f3,
			com.mojang.blaze3d.buffers.GpuBufferSlice gpuBufferSlice,
			org.joml.Vector4f vector4f,
			boolean bl2,
			CallbackInfo ci
	) {
		ClientRenderStages.fireRenderEnd();
	}

	@Inject(method = "extractVisibleEntities", at = @At("RETURN"), require = 0)
	private void nitrium$beforeEntities(
			Camera camera,
			Frustum frustum,
			DeltaTracker deltaTracker,
			LevelRenderState levelRenderState,
			CallbackInfo ci
	) {
		ClientRenderStages.fireBeforeEntities();
	}

	@Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/debug/DebugRenderer;emitGizmos(Lnet/minecraft/client/renderer/culling/Frustum;DDDF)V", shift = At.Shift.AFTER), require = 0)
	private void nitrium$beforeDebug(
			DeltaTracker deltaTracker,
			boolean bl,
			Camera camera,
			Matrix4f matrix4f,
			Matrix4f matrix4f2,
			Matrix4f matrix4f3,
			com.mojang.blaze3d.buffers.GpuBufferSlice gpuBufferSlice,
			org.joml.Vector4f vector4f,
			boolean bl2,
			CallbackInfo ci
	) {
		ClientRenderStages.fireBeforeDebug();
	}

	@Inject(method = "submitEntities", at = @At("RETURN"), require = 0)
	private void nitrium$afterEntities(
			com.mojang.blaze3d.vertex.PoseStack poseStack,
			LevelRenderState levelRenderState,
			net.minecraft.client.renderer.SubmitNodeCollector submitNodeCollector,
			CallbackInfo ci
	) {
		ClientRenderStages.fireAfterEntities();
	}
}
