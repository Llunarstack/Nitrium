package dev.nitrium.client.streaming;

import dev.nitrium.NitriumMod;
import dev.nitrium.config.NitriumConfigManager;

/**
 * Unified megabuffer pool for indirect multi-draw submission. For now it only accounts for the
 * byte budget; the actual GL buffer allocation lands with the Sodium integration.
 */
public final class GeometryBufferPool {
	private static GeometryBufferPool instance;

	private final int budgetBytes;
	private int allocatedBytes;

	private GeometryBufferPool(int budgetBytes) {
		this.budgetBytes = budgetBytes;
	}

	public static void init() {
		if (instance != null) {
			return;
		}

		int budgetMb = NitriumConfigManager.get().geometryBufferBudgetMb;
		instance = new GeometryBufferPool(budgetMb * 1024 * 1024);
		NitriumMod.LOGGER.info("Nitrium geometry buffer budget: {} MB", budgetMb);
	}

	public static GeometryBufferPool get() {
		return instance;
	}

	public int budgetBytes() {
		return budgetBytes;
	}

	public int allocatedBytes() {
		return allocatedBytes;
	}

	public int remainingBytes() {
		return Math.max(0, budgetBytes - allocatedBytes);
	}

	/**
	 * Reserve budget for one section mesh. TODO: back this with a real slice of a shared SSBO.
	 */
	public boolean tryReserve(int bytes) {
		if (allocatedBytes + bytes > budgetBytes) {
			return false;
		}
		allocatedBytes += bytes;
		return true;
	}

	public void release(int bytes) {
		allocatedBytes = Math.max(0, allocatedBytes - bytes);
	}
}
