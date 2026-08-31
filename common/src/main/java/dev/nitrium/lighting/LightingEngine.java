package dev.nitrium.lighting;

import dev.nitrium.NitriumMod;
import dev.nitrium.config.NitriumConfigManager;
import dev.nitrium.platform.ServerEvents;
import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.Map;

/**
 * Async bitpacked lighting engine with DAG propagation and update batching.
 */
public final class LightingEngine {
	private static LightingEngine instance;

	private final Map<Long, BitpackedLightSection> sections = new HashMap<>();
	private final LightPropagationDag dag = new LightPropagationDag();
	private final LightUpdateBatcher batcher = new LightUpdateBatcher();
	private final AsyncLightWorkerPool workers;
	private final LightingStats stats = new LightingStats();

	private LightingEngine() {
		this.workers = AsyncLightWorkerPool.create();
	}

	public static void init() {
		if (instance != null) {
			return;
		}
		instance = new LightingEngine();
		instance.register();
	}

	private void register() {
		ServerEvents events = ServerEvents.get();
		events.serverTickStart(server -> onServerTickStart(server.getTickCount()));
		events.serverTickEnd(server -> {
			if (!NitriumConfigManager.get().enableLightUpdateBatching) {
				return;
			}
			if (batcher.isDue(server.getTickCount())) {
				LightUpdateRegion region = batcher.flush();
				if (region != null) {
					schedulePropagation(region);
				}
			}
		});

		NitriumMod.LOGGER.info("Nitrium lighting engine active (workers={})",
				NitriumConfigManager.get().lightWorkerThreads);
	}

	/**
	 * Called from mixin when vanilla schedules a block/sky light update.
	 */
	public void onLightUpdate(BlockPos pos) {
		stats.recordUpdate();
		if (NitriumConfigManager.get().enableLightUpdateBatching) {
			batcher.include(pos.getX(), pos.getY(), pos.getZ());
			if (batcher.scheduledTick() < 0) {
				batcher.scheduleForTick(-1); // will be set on next tick
			}
		} else {
			workers.submit(() -> propagateSingle(pos));
		}
	}

	public void onServerTickStart(int tick) {
		if (NitriumConfigManager.get().enableLightUpdateBatching && batcher.hasPending() && batcher.scheduledTick() < 0) {
			batcher.scheduleForTick(tick + 1);
		}
	}

	private void schedulePropagation(LightUpdateRegion region) {
		stats.recordBatch(region.updateCount());
		workers.submit(() -> {
			// TODO: run the bitpacked BFS across the merged region.
			stats.recordPropagation(region.volumeEstimate());
		});
	}

	private void propagateSingle(BlockPos pos) {
		long key = sectionKey(pos);
		sections.computeIfAbsent(key, ignored -> new BitpackedLightSection());
		stats.recordPropagation(1);
	}

	private static long sectionKey(BlockPos pos) {
		int sx = pos.getX() >> 4;
		int sy = pos.getY() >> 4;
		int sz = pos.getZ() >> 4;
		return ((long) sx & 0xFFFFF) | (((long) sy & 0xFFFFF) << 20) | (((long) sz & 0xFFFFF) << 40);
	}

	public BitpackedLightSection section(long key) {
		return sections.get(key);
	}

	public LightingStats stats() {
		return stats;
	}

	public static LightingEngine get() {
		return instance;
	}

	public void onWorldUnload() {
		sections.clear();
		dag.clear();
		batcher.reset();
		stats.reset();
	}

	public void shutdown() {
		workers.shutdown();
		instance = null;
	}
}
