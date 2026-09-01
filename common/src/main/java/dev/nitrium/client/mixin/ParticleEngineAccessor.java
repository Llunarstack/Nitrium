package dev.nitrium.client.mixin;

import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.ParticleEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(ParticleEngine.class)
public interface ParticleEngineAccessor {
	@Accessor("particles")
	Map<ParticleRenderType, ParticleGroup<?>> nitrium$getParticleGroups();
}
