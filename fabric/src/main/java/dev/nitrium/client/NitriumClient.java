package dev.nitrium.client;

import dev.nitrium.client.audio.NitriumAudio;
import dev.nitrium.client.gui.NitriumGui;
import dev.nitrium.client.itempool.NitriumClientItemPooling;
import dev.nitrium.client.memory.NitriumClientLifecycle;
import dev.nitrium.client.network.NitriumClientNetwork;
import dev.nitrium.client.nativegl.NitriumAzdoBackend;
import dev.nitrium.client.culling.NitriumCulling;
import dev.nitrium.client.entity.EntityRenderOptimizer;
import dev.nitrium.client.particles.NitriumParticles;
import dev.nitrium.client.render.oit.NitriumOit;
import dev.nitrium.client.streaming.NitriumStreaming;
import dev.nitrium.config.NitriumConfigManager;
import dev.nitrium.client.fabric.FabricClientEvents;
import dev.nitrium.client.platform.ClientEvents;
import dev.nitrium.client.profiling.NitriumProfiler;
import dev.nitrium.client.profiling.ProfilingDebugOverlay;
import net.fabricmc.api.ClientModInitializer;

public class NitriumClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientEvents.install(new FabricClientEvents());

		NitriumConfigManager.load();
		NitriumProfiler.init();
		ProfilingDebugOverlay.register();
		NitriumStreaming.init();
		NitriumCulling.init();
		NitriumOit.init();
		NitriumParticles.init();
		NitriumGui.init();
		NitriumAudio.init();
		EntityRenderOptimizer.init();
		NitriumAzdoBackend.init();
		NitriumClientItemPooling.init();
		NitriumClientNetwork.init();
		NitriumClientLifecycle.init();
	}
}
