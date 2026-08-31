package dev.nitrium.redstone;

import dev.nitrium.Nitrium;
import dev.nitrium.config.NitriumConfigManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * Orchestrates topological redstone solving and block-entity sleep states.
 */
public final class RedstoneOptimizationEngine {
	private static RedstoneOptimizationEngine instance;

	private final TopologicalRedstoneGraph wireGraph = new TopologicalRedstoneGraph();
	private final BlockEntitySleepRegistry sleepRegistry = new BlockEntitySleepRegistry();
	private final RedstoneStats stats = new RedstoneStats();

	private RedstoneOptimizationEngine() {
	}

	public static void init() {
		if (instance != null) {
			return;
		}
		instance = new RedstoneOptimizationEngine();
		Nitrium.LOGGER.info("Nitrium redstone engine active (topological={}, sleep={})",
				NitriumConfigManager.get().enableTopologicalRedstone,
				NitriumConfigManager.get().enableBlockEntitySleep);
	}

	public void onWireUpdate(Level level, BlockPos pos) {
		stats.recordWireUpdate();
		if (!NitriumConfigManager.get().enableTopologicalRedstone) {
			return;
		}
		// TODO: collect the contiguous wire network and solve it in a single pass.
		wireGraph.clear();
	}

	public TopologicalRedstoneGraph wireGraph() {
		return wireGraph;
	}

	public BlockEntitySleepRegistry sleepRegistry() {
		return sleepRegistry;
	}

	public RedstoneStats stats() {
		return stats;
	}

	public static RedstoneOptimizationEngine get() {
		return instance;
	}

	public void onWorldUnload() {
		wireGraph.clear();
		sleepRegistry.clear();
		stats.reset();
	}
}
