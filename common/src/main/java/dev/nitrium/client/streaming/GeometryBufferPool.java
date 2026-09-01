package dev.nitrium.client.streaming;

import dev.nitrium.Nitrium;
import dev.nitrium.client.nativegl.NitriumAzdoBackend;
import dev.nitrium.client.nativegl.PersistentMappedBuffer;
import dev.nitrium.config.NitriumConfigManager;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Unified geometry buffer pool backed by the AZDO persistent mapped buffer when available.
 */
public final class GeometryBufferPool {
	private static GeometryBufferPool instance;

	private final int budgetBytes;
	private final AtomicInteger allocatedBytes = new AtomicInteger();

	private GeometryBufferPool(int budgetBytes) {
		this.budgetBytes = budgetBytes;
	}

	public static void init() {
		if (instance != null) {
			return;
		}

		int budgetMb = NitriumConfigManager.get().geometryBufferBudgetMb;
		instance = new GeometryBufferPool(budgetMb * 1024 * 1024);
		Nitrium.LOGGER.info("Nitrium geometry buffer budget: {} MB", budgetMb);
	}

	public static GeometryBufferPool get() {
		return instance;
	}

	public int budgetBytes() {
		return budgetBytes;
	}

	public int allocatedBytes() {
		return allocatedBytes.get();
	}

	public int remainingBytes() {
		return Math.max(0, budgetBytes - allocatedBytes.get());
	}

	/**
	 * Reserve a slice from the shared persistent geometry buffer.
	 *
	 * @return byte offset into the geometry buffer, or -1 if unavailable
	 */
	public int tryReserve(int bytes) {
		if (bytes <= 0) {
			return -1;
		}

		int current;
		do {
			current = allocatedBytes.get();
			if (current + bytes > budgetBytes) {
				return -1;
			}
		} while (!allocatedBytes.compareAndSet(current, current + bytes));

		NitriumAzdoBackend backend = NitriumAzdoBackend.get();
		if (backend == null || backend.geometryBuffer() == null) {
			release(bytes);
			return -1;
		}

		return current;
	}

	public boolean writeSlice(int offset, ByteBuffer data) {
		NitriumAzdoBackend backend = NitriumAzdoBackend.get();
		if (backend == null) {
			return false;
		}

		PersistentMappedBuffer buffer = backend.geometryBuffer();
		if (buffer == null) {
			return false;
		}

		buffer.write(offset, data);
		return true;
	}

	public void release(int bytes) {
		allocatedBytes.updateAndGet(current -> Math.max(0, current - bytes));
	}
}
