package dev.nitrium.client.particles;

import dev.nitrium.NitriumMod;
import dev.nitrium.config.NitriumConfigManager;
import dev.nitrium.memory.NativeResourceCleaner;
import dev.nitrium.client.platform.ClientEvents;

import java.lang.ref.Cleaner;

/**
 * GPU compute particle simulation with SSBO storage and indirect instanced draw.
 */
public final class GpuParticleEngine {
	private static GpuParticleEngine instance;

	private ParticleSsboPool ssboPool;
	private ParticleIndirectDraw indirectDraw;
	private final ParticleStats stats = new ParticleStats();
	private final Cleaner.Cleanable cleanable;

	private GpuParticleEngine() {
		this.cleanable = NativeResourceCleaner.register(this, this::destroyGpuResources);
	}

	public static void init() {
		if (instance != null) {
			return;
		}
		instance = new GpuParticleEngine();
		instance.register();
	}

	private void register() {
		int max = NitriumConfigManager.get().maxGpuParticles;
		ssboPool = new ParticleSsboPool(max);
		indirectDraw = new ParticleIndirectDraw();

		ClientEvents events = ClientEvents.get();
		events.worldRenderBeforeDebug(this::simulateAndCull);
		events.worldRenderAfterEntities(this::drawParticles);

		NitriumMod.LOGGER.info("Nitrium GPU particle engine active (max={})", max);
	}

	private void simulateAndCull() {
		stats.recordSimulatePass();
		// TODO: dispatch the compute shader for particle physics + frustum/occlusion cull.
		ssboPool.setActiveCount(0);
	}

	private void drawParticles() {
		int count = ssboPool.activeCount();
		if (count > 0) {
			ssboPool.uploadStaging();
			indirectDraw.drawInstanced(count);
			stats.recordDraw(count);
		}
	}

	public ParticleStats stats() {
		return stats;
	}

	public static GpuParticleEngine get() {
		return instance;
	}

	public void onWorldUnload() {
		stats.reset();
		if (ssboPool != null) {
			ssboPool.setActiveCount(0);
		}
	}

	public void shutdown() {
		cleanable.clean();
		instance = null;
	}

	private void destroyGpuResources() {
		if (ssboPool != null) {
			ssboPool.close();
			ssboPool = null;
		}
		if (indirectDraw != null) {
			indirectDraw.close();
			indirectDraw = null;
		}
	}
}
