package dev.nitrium.compat;

import dev.nitrium.config.NitriumConfigManager;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Conditionally applies Nitrium mixins so we never double-patch classes already
 * optimized by Lithium, Starlight, C2ME, Clumps, etc.
 */
public final class NitriumMixinPlugin implements IMixinConfigPlugin {
	private static final Map<String, NitriumFeature> MIXIN_FEATURES = Map.ofEntries(
			Map.entry("EntityTickThrottleMixin", NitriumFeature.ENTITY_OPTIMIZATION),
			Map.entry("LivingEntityAiThrottleMixin", NitriumFeature.ENTITY_OPTIMIZATION),
			Map.entry("ChunkGenerationTaskMixin", NitriumFeature.WORLDGEN_OPTIMIZATION),
			Map.entry("ThreadedLevelLightEngineMixin", NitriumFeature.LIGHTING_ENGINE),
			Map.entry("HopperBlockEntityMixin", NitriumFeature.BLOCK_ENTITY_SLEEP),
			Map.entry("AbstractFurnaceBlockEntityMixin", NitriumFeature.BLOCK_ENTITY_SLEEP),
			Map.entry("RedStoneWireBlockMixin", NitriumFeature.REDSTONE_TOPOLOGICAL),
			Map.entry("ExperienceOrbMixin", NitriumFeature.ITEM_XP_POOLING),
			Map.entry("ItemEntityMixin", NitriumFeature.ITEM_XP_POOLING),
			Map.entry("ExperienceOrbInvoker", NitriumFeature.ITEM_XP_POOLING),
			Map.entry("ItemEntityInvoker", NitriumFeature.ITEM_XP_POOLING)
	);

	@Override
	public void onLoad(String mixinPackage) {
		NitriumConfigManager.load();
		ModCompatibility.init();
	}

	@Override
	public String getRefMapperConfig() {
		return null;
	}

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		String simpleName = mixinClassName.substring(mixinClassName.lastIndexOf('.') + 1);
		NitriumFeature feature = MIXIN_FEATURES.get(simpleName);
		if (feature == null) {
			return true;
		}
		return ModCompatibility.isActive(feature);
	}

	@Override
	public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
	}

	@Override
	public List<String> getMixins() {
		return null;
	}

	@Override
	public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}

	@Override
	public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}
}
