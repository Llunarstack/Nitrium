package dev.nitrium.client.memory;

import dev.nitrium.NitriumMod;
import dev.nitrium.client.culling.CullingPipeline;
import dev.nitrium.client.culling.terrain.HiZOcclusionCuller;
import dev.nitrium.client.entity.EntityRenderOptimizer;
import dev.nitrium.client.audio.AsyncAudioOcclusionEngine;
import dev.nitrium.client.gui.GuiRenderEngine;
import dev.nitrium.client.particles.GpuParticleEngine;
import dev.nitrium.client.render.oit.WeightedBlendedOitPipeline;
import dev.nitrium.client.itempool.NitriumClientItemPooling;
import dev.nitrium.client.nativegl.NitriumAzdoBackend;
import dev.nitrium.client.streaming.AsyncChunkCacheStore;
import dev.nitrium.client.streaming.StreamingChunkLoader;
import dev.nitrium.nativecore.NativeBootstrap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

/**
 * Client lifecycle cleanup for GPU buffers, caches, and profiling state.
 */
public final class NitriumClientLifecycle {
	private static boolean initialized;
	private static ClientLevel activeWorld;

	private NitriumClientLifecycle() {
	}

	public static void init() {
		if (initialized) {
			return;
		}

		initialized = true;

		ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register(NitriumClientLifecycle::onClientWorldChange);
		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> onClientStopping());

		NitriumMod.LOGGER.info("Nitrium client lifecycle hooks registered");
	}

	private static void onClientWorldChange(Minecraft client, ClientLevel newWorld) {
		ClientLevel previous = activeWorld;
		activeWorld = newWorld;
		if (previous != null) {
			flushClientCaches(previous);
		}
	}

	private static void flushClientCaches(ClientLevel level) {
		CullingPipeline culling = CullingPipeline.get();
		if (culling != null) {
			culling.onWorldUnload();
		}

		EntityRenderOptimizer optimizer = EntityRenderOptimizer.get();
		if (optimizer != null) {
			optimizer.onWorldUnload();
		}

		StreamingChunkLoader streaming = StreamingChunkLoader.get();
		if (streaming != null) {
			streaming.onWorldUnload();
		}

		NitriumClientItemPooling itemPooling = NitriumClientItemPooling.get();
		if (itemPooling != null) {
			itemPooling.onWorldUnload();
		}

		WeightedBlendedOitPipeline oit = WeightedBlendedOitPipeline.get();
		if (oit != null) {
			oit.onWorldUnload();
		}

		GpuParticleEngine particles = GpuParticleEngine.get();
		if (particles != null) {
			particles.onWorldUnload();
		}

		GuiRenderEngine gui = GuiRenderEngine.get();
		if (gui != null) {
			gui.onWorldUnload();
		}

		AsyncAudioOcclusionEngine audio = AsyncAudioOcclusionEngine.get();
		if (audio != null) {
			audio.onWorldUnload();
		}

		if (level != null) {
			NitriumMod.LOGGER.debug("Nitrium flushed client caches for {}", level.dimension().identifier());
		} else {
			NitriumMod.LOGGER.debug("Nitrium flushed client caches after world disconnect");
		}
	}

	private static void onClientStopping() {
		AsyncChunkCacheStore cache = AsyncChunkCacheStore.get();
		if (cache != null) {
			cache.shutdown();
		}

		NitriumAzdoBackend azdo = NitriumAzdoBackend.get();
		if (azdo != null) {
			azdo.close();
		}

		WeightedBlendedOitPipeline oit = WeightedBlendedOitPipeline.get();
		if (oit != null) {
			oit.shutdown();
		}

		GpuParticleEngine particles = GpuParticleEngine.get();
		if (particles != null) {
			particles.shutdown();
		}

		GuiRenderEngine gui = GuiRenderEngine.get();
		if (gui != null) {
			gui.shutdown();
		}

		AsyncAudioOcclusionEngine audio = AsyncAudioOcclusionEngine.get();
		if (audio != null) {
			audio.shutdown();
		}

		NativeBootstrap.shutdown();
		NitriumMod.LOGGER.info("Nitrium client shutdown complete");
	}

	/** Called when Iris or another shader pack reloads. */
	public static void onShaderPackReload() {
		HiZOcclusionCuller culler = HiZOcclusionCuller.get();
		if (culler != null) {
			culler.invalidatePyramid();
		}

		WeightedBlendedOitPipeline oit = WeightedBlendedOitPipeline.get();
		if (oit != null) {
			oit.onWorldUnload();
		}

		NitriumMod.LOGGER.debug("Nitrium invalidated GPU caches after shader reload");
	}
}
