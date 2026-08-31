package dev.nitrium;

import dev.nitrium.compat.ModCompatibility;
import dev.nitrium.config.NitriumConfigManager;
import dev.nitrium.itempool.NitriumItemPooling;
import dev.nitrium.lighting.NitriumLighting;
import dev.nitrium.layout.NitriumMemoryLayout;
import dev.nitrium.memory.NitriumLifecycle;
import dev.nitrium.network.NitriumNetwork;
import dev.nitrium.redstone.NitriumRedstone;
import dev.nitrium.storage.NitriumStorage;
import dev.nitrium.worldgen.NitriumWorldgen;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NitriumMod implements ModInitializer {
	public static final String MOD_ID = "nitrium";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		NitriumConfigManager.load();
		ModCompatibility.init();
		// Load the native core before subsystems that probe native availability at init
		// time (e.g. async chunk storage), otherwise they latch onto the Java fallback.
		dev.nitrium.nativecore.NativeBootstrap.init();
		dev.nitrium.entity.EntityOptimizationEngine.init();
		NitriumWorldgen.init();
		NitriumLighting.init();
		NitriumRedstone.init();
		NitriumItemPooling.init();
		NitriumNetwork.init();
		NitriumMemoryLayout.init();
		NitriumStorage.init();
		NitriumLifecycle.init();
		LOGGER.info("Nitrium performance mod initializing");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
