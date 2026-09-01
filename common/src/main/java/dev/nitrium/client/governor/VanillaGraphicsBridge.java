package dev.nitrium.client.governor;

import dev.nitrium.Nitrium;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.server.level.ParticleStatus;

/**
 * Maps a {@link ShaderProfile} onto vanilla graphics options (entity shadows, particles) so the
 * shader governor still does something visible without a shader pack. Graphics presets (fast/fancy)
 * are intentionally not touched — {@code applyGraphicsPreset} rebuilds every loaded chunk.
 * With Iris present, the richer profile fields go to the Iris bridge instead.
 */
public final class VanillaGraphicsBridge {
	private Boolean lastEntityShadows;
	private ParticleStatus lastParticles;

	/**
	 * @return {@code true} if any vanilla option was changed
	 */
	public boolean apply(ShaderProfile profile) {
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.options == null) {
			return false;
		}

		Options options = client.options;
		boolean changed = false;

		boolean desiredEntityShadows = profile.entityShadows();
		if (options.entityShadows().get() != desiredEntityShadows
				&& !Boolean.valueOf(desiredEntityShadows).equals(lastEntityShadows)) {
			options.entityShadows().set(desiredEntityShadows);
			lastEntityShadows = desiredEntityShadows;
			changed = true;
		}

		ParticleStatus desiredParticles = particlesFor(profile.level());
		if (options.particles().get() != desiredParticles && desiredParticles != lastParticles) {
			options.particles().set(desiredParticles);
			lastParticles = desiredParticles;
			changed = true;
		}

		if (changed) {
			Nitrium.LOGGER.debug("Nitrium vanilla graphics: entityShadows={}, particles={}",
					desiredEntityShadows, desiredParticles);
		}
		return changed;
	}

	private static ParticleStatus particlesFor(ShaderQualityLevel level) {
		return switch (level) {
			case SURVIVAL -> ParticleStatus.MINIMAL;
			case PERFORMANCE -> ParticleStatus.DECREASED;
			case BALANCED, HIGH, CINEMATIC -> ParticleStatus.ALL;
		};
	}

	public void reset() {
		lastEntityShadows = null;
		lastParticles = null;
	}
}
