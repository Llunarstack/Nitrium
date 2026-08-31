package dev.nitrium.memory;

import dev.nitrium.NitriumMod;
import dev.nitrium.entity.EntityOptimizationEngine;
import dev.nitrium.itempool.ItemXpPoolingEngine;
import dev.nitrium.lighting.LightingEngine;
import dev.nitrium.layout.MemoryLayoutEngine;
import dev.nitrium.network.PacketPipeline;
import dev.nitrium.redstone.RedstoneOptimizationEngine;
import dev.nitrium.worldgen.WorldgenOptimizationEngine;
import dev.nitrium.storage.AsyncChunkStorageEngine;
import dev.nitrium.nativecore.NativeBootstrap;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

/**
 * Central lifecycle hooks that flush caches and release native resources on unload.
 */
public final class NitriumLifecycle {
	private static boolean initialized;

	private NitriumLifecycle() {
	}

	public static void init() {
		if (initialized) {
			return;
		}

		initialized = true;

		ServerWorldEvents.UNLOAD.register(NitriumLifecycle::onServerWorldUnload);
		ServerLifecycleEvents.SERVER_STOPPING.register(NitriumLifecycle::onServerStopping);

		NitriumMod.LOGGER.info("Nitrium lifecycle hooks registered");
	}

	private static void onServerWorldUnload(MinecraftServer server, ServerLevel level) {
		EntityOptimizationEngine engine = EntityOptimizationEngine.get();
		if (engine != null) {
			engine.onWorldUnload();
		}

		WorldgenOptimizationEngine worldgen = WorldgenOptimizationEngine.get();
		if (worldgen != null) {
			worldgen.onWorldUnload();
		}

		LightingEngine lighting = LightingEngine.get();
		if (lighting != null) {
			lighting.onWorldUnload();
		}

		RedstoneOptimizationEngine redstone = RedstoneOptimizationEngine.get();
		if (redstone != null) {
			redstone.onWorldUnload();
		}

		ItemXpPoolingEngine pooling = ItemXpPoolingEngine.get();
		if (pooling != null) {
			pooling.onWorldUnload();
		}

		MemoryLayoutEngine layout = MemoryLayoutEngine.get();
		if (layout != null) {
			layout.onWorldUnload();
		}

		AsyncChunkStorageEngine storage = AsyncChunkStorageEngine.get();
		if (storage != null) {
			storage.onWorldUnload();
		}

		NitriumMod.LOGGER.debug("Nitrium flushed server caches for {}", level.dimension().identifier());
	}

	private static void onServerStopping(MinecraftServer server) {
		WorldgenOptimizationEngine worldgen = WorldgenOptimizationEngine.get();
		if (worldgen != null) {
			worldgen.shutdown();
		}

		LightingEngine lighting = LightingEngine.get();
		if (lighting != null) {
			lighting.shutdown();
		}

		PacketPipeline network = PacketPipeline.get();
		if (network != null) {
			network.shutdown();
		}

		AsyncChunkStorageEngine storage = AsyncChunkStorageEngine.get();
		if (storage != null) {
			storage.shutdown();
		}

		NativeBootstrap.shutdown();
		NitriumMod.LOGGER.info("Nitrium server shutdown complete");
	}
}
