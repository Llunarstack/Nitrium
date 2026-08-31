package dev.nitrium.compat;

import dev.nitrium.NitriumMod;
import dev.nitrium.config.NitriumConfig;
import dev.nitrium.config.NitriumConfigManager;
import dev.nitrium.platform.Platform;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Detects known performance mods and defers overlapping Nitrium features to prevent
 * double-patching, conflicting tick cancellations, and redundant work.
 */
public final class ModCompatibility {
	private static final Map<NitriumFeature, List<String>> KNOWN_CONFLICTS = new EnumMap<>(NitriumFeature.class);

	private static EnumSet<NitriumFeature> deferred = EnumSet.noneOf(NitriumFeature.class);
	private static final Map<String, String> loadedConflictMods = new LinkedHashMap<>();
	private static boolean initialized;

	static {
		register(NitriumFeature.ENTITY_OPTIMIZATION, "lithium");
		register(NitriumFeature.BLOCK_ENTITY_SLEEP, "lithium");
		register(NitriumFeature.REDSTONE_TOPOLOGICAL, "lithium", "alternate-current", "wirnet");
		register(NitriumFeature.LIGHTING_ENGINE, "starlight");
		register(NitriumFeature.WORLDGEN_OPTIMIZATION, "c2me");
		register(NitriumFeature.MEMORY_LAYOUT, "ferritecore", "modernfix");
		register(NitriumFeature.NETWORK_PIPELINE, "krypton");
		register(NitriumFeature.ITEM_XP_POOLING, "clumps");
		register(NitriumFeature.SECTION_DISK_CACHE, "bobby");
		register(NitriumFeature.GPU_ENTITY_OCCLUSION, "entityculling");
	}

	private ModCompatibility() {
	}

	private static void register(NitriumFeature feature, String... modIds) {
		KNOWN_CONFLICTS.computeIfAbsent(feature, ignored -> new ArrayList<>()).addAll(List.of(modIds));
	}

	public static void init() {
		if (initialized) {
			return;
		}
		initialized = true;

		NitriumConfig config = NitriumConfigManager.get();
		if (!config.enableCompatibilityAutoDisable) {
			NitriumMod.LOGGER.info("Nitrium compatibility auto-disable is off — all features may overlap with other mods");
			return;
		}

		for (var entry : KNOWN_CONFLICTS.entrySet()) {
			NitriumFeature feature = entry.getKey();
			for (String modId : entry.getValue()) {
				if (Platform.isModLoaded(modId)) {
					defer(feature, modId);
					break;
				}
			}
		}

		logReport();
	}

	private static void defer(NitriumFeature feature, String modId) {
		if (deferred.add(feature)) {
			loadedConflictMods.put(feature.name(), modId);
		}
	}

	public static boolean isActive(NitriumFeature feature) {
		NitriumConfig config = NitriumConfigManager.get();
		if (!isConfigEnabled(feature, config)) {
			return false;
		}
		if (config.compatibilityForceEnableAll) {
			return true;
		}
		if (!config.enableCompatibilityAutoDisable) {
			return true;
		}
		return !deferred.contains(feature);
	}

	public static boolean isDeferred(NitriumFeature feature) {
		return deferred.contains(feature);
	}

	public static String conflictingMod(NitriumFeature feature) {
		return loadedConflictMods.get(feature.name());
	}

	public static Set<NitriumFeature> deferredFeatures() {
		return EnumSet.copyOf(deferred);
	}

	public static boolean isSodiumLoaded() {
		return Platform.isModLoaded("sodium");
	}

	public static boolean isIrisLoaded() {
		return Platform.isModLoaded("iris");
	}

	private static boolean isConfigEnabled(NitriumFeature feature, NitriumConfig config) {
		return switch (feature) {
			case ENTITY_OPTIMIZATION -> config.enableEntityOptimization;
			case LIGHTING_ENGINE -> config.enableLightingEngine;
			case WORLDGEN_OPTIMIZATION -> config.enableWorldgenOptimization;
			case REDSTONE_TOPOLOGICAL -> config.enableTopologicalRedstone;
			case BLOCK_ENTITY_SLEEP -> config.enableBlockEntitySleep;
			case ITEM_XP_POOLING -> config.enableItemXpPooling;
			case NETWORK_PIPELINE -> config.enableNetworkPipeline;
			case MEMORY_LAYOUT -> config.enableMemoryLayoutOptimization;
			case SECTION_DISK_CACHE -> config.enableSectionDiskCache;
			case GPU_ENTITY_OCCLUSION -> config.enableEntityOcclusion;
		};
	}

	private static void logReport() {
		if (deferred.isEmpty()) {
			NitriumMod.LOGGER.info("Nitrium compatibility: no conflicting performance mods detected");
			return;
		}

		StringBuilder builder = new StringBuilder("Nitrium deferred overlapping features: ");
		boolean first = true;
		for (NitriumFeature feature : deferred) {
			if (!first) {
				builder.append(", ");
			}
			builder.append(feature.name().toLowerCase())
					.append(" (")
					.append(loadedConflictMods.get(feature.name()))
					.append(')');
			first = false;
		}
		NitriumMod.LOGGER.info(builder.toString());

		List<String> complements = new ArrayList<>();
		if (isSodiumLoaded()) {
			complements.add("sodium");
		}
		if (isIrisLoaded()) {
			complements.add("iris");
		}
		if (!complements.isEmpty()) {
			NitriumMod.LOGGER.info("Nitrium complements: {} — GPU culling/streaming remain active",
					String.join(", ", complements));
		}
	}
}
