package dev.nitrium.client.governor;

import dev.nitrium.NitriumMod;
import net.minecraft.client.GraphicsPreset;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.server.level.ParticleStatus;

/**
 * Maps a {@link ShaderProfile} onto vanilla graphics options (preset, entity shadows, particles) so
 * the shader governor still does something visible without a shader pack. Only slow-loop knobs are
 * touched, and the controller's cooldown keeps fast/fancy toggles — which rebuild chunks — rare.
 * With Iris present, the richer profile fields go to the Iris bridge instead.
 */
public final class VanillaGraphicsBridge {
	private GraphicsPreset lastPreset;
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

		GraphicsPreset desiredPreset = presetFor(profile.level());
		if (options.graphicsPreset().get() != desiredPreset && desiredPreset != lastPreset) {
			// applyGraphicsPreset cascades the preset to leaves/water/clouds sub-options.
			options.applyGraphicsPreset(desiredPreset);
			lastPreset = desiredPreset;
			changed = true;
		}

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
			NitriumMod.LOGGER.debug("Nitrium vanilla graphics: preset={}, entityShadows={}, particles={}",
					desiredPreset, desiredEntityShadows, desiredParticles);
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

	private static GraphicsPreset presetFor(ShaderQualityLevel level) {
		return switch (level) {
			case SURVIVAL, PERFORMANCE -> GraphicsPreset.FAST;
			case BALANCED, HIGH -> GraphicsPreset.FANCY;
			case CINEMATIC -> GraphicsPreset.FABULOUS;
		};
	}

	public void reset() {
		lastPreset = null;
		lastEntityShadows = null;
		lastParticles = null;
	}
}
