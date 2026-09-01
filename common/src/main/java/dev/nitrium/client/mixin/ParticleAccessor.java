package dev.nitrium.client.mixin;

import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Particle.class)
public interface ParticleAccessor {
	@Accessor("x")
	double nitrium$getX();

	@Accessor("y")
	double nitrium$getY();

	@Accessor("z")
	double nitrium$getZ();

	@Accessor("xd")
	double nitrium$getXd();

	@Accessor("yd")
	double nitrium$getYd();

	@Accessor("zd")
	double nitrium$getZd();

	@Accessor("lifetime")
	int nitrium$getLifetime();

	@Accessor("removed")
	boolean nitrium$isRemoved();
}
