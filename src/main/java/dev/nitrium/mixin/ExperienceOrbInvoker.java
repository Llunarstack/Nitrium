package dev.nitrium.mixin;

import net.minecraft.world.entity.ExperienceOrb;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ExperienceOrb.class)
public interface ExperienceOrbInvoker {
	@Invoker("canMerge")
	boolean nitrium$canMerge(ExperienceOrb other);

	// Target ExperienceOrb.canMerge(ExperienceOrb, int, int) is static, so this invoker must be too.
	@Invoker("canMerge")
	static boolean nitrium$canMergeValues(ExperienceOrb other, int selfValue, int otherValue) {
		throw new AssertionError("Mixin @Invoker not applied");
	}

	@Invoker("merge")
	void nitrium$merge(ExperienceOrb other);
}
