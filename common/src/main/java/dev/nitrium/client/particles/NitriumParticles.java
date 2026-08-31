package dev.nitrium.client.particles;

import dev.nitrium.Nitrium;
import dev.nitrium.config.NitriumConfigManager;

/**
 * Facade for GPU compute particle simulation.
 */
public final class NitriumParticles {
	private NitriumParticles() {
	}

	public static void init() {
		if (!NitriumConfigManager.get().enableGpuParticles) {
			Nitrium.LOGGER.info("Nitrium GPU particles disabled via config");
			return;
		}
		GpuParticleEngine.init();
	}

	public static GpuParticleEngine get() {
		return GpuParticleEngine.get();
	}
}
