package dev.nitrium.client.mixin;

import com.mojang.blaze3d.textures.GpuSampler;
import dev.nitrium.client.platform.ClientRenderStages;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkSectionsToRender.class)
public abstract class ChunkSectionsToRenderMixin {
	@Inject(method = "renderGroup", at = @At("HEAD"), require = 0)
	private void nitrium$beforeGroup(ChunkSectionLayerGroup group, GpuSampler sampler, CallbackInfo ci) {
		if (group == ChunkSectionLayerGroup.OPAQUE) {
			ClientRenderStages.fireAfterOpaque();
		} else if (group == ChunkSectionLayerGroup.TRANSLUCENT) {
			ClientRenderStages.fireBeforeTranslucent();
		}
	}

	@Inject(method = "renderGroup", at = @At("RETURN"), require = 0)
	private void nitrium$afterGroup(ChunkSectionLayerGroup group, GpuSampler sampler, CallbackInfo ci) {
		if (group == ChunkSectionLayerGroup.TRANSLUCENT) {
			ClientRenderStages.fireAfterTranslucent();
		}
	}
}
