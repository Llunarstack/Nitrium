package dev.nitrium.client.itempool;

import dev.nitrium.Nitrium;
import dev.nitrium.config.NitriumConfigManager;

import java.util.HashSet;
import java.util.Set;

/**
 * Tracks item entities rendered as GPU billboards beyond the tick distance.
 */
public final class DistantItemBillboardRenderer {
	private final Set<Integer> billboardEntityIds = new HashSet<>();

	public boolean shouldUseBillboard(int entityId, double distanceSq) {
		int threshold = NitriumConfigManager.get().distantItemBillboardBlocks;
		return distanceSq > (long) threshold * threshold;
	}

	public void markBillboard(int entityId) {
		billboardEntityIds.add(entityId);
	}

	public boolean isBillboard(int entityId) {
		return billboardEntityIds.contains(entityId);
	}

	public void clear() {
		billboardEntityIds.clear();
	}

	public int activeBillboards() {
		return billboardEntityIds.size();
	}
}
