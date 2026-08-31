package dev.nitrium.worldgen.pool;

/**
 * Per-thread scratch buffers for worldgen passes — no BlockPos or boxed allocations.
 */
public final class ZeroAllocWorldgenContext {
	private static final ThreadLocal<ZeroAllocWorldgenContext> THREAD_LOCAL =
			ThreadLocal.withInitial(ZeroAllocWorldgenContext::new);

	private final PrimitiveBufferPool pool = new PrimitiveBufferPool();

	private ZeroAllocWorldgenContext() {
	}

	public static ZeroAllocWorldgenContext current() {
		return THREAD_LOCAL.get();
	}

	public float[] borrowFloats(int size) {
		return pool.borrowFloats(size);
	}

	public int[] borrowInts(int size) {
		return pool.borrowInts(size);
	}

	public long[] borrowLongs(int size) {
		return pool.borrowLongs(size);
	}

	public byte[] borrowBytes(int size) {
		return pool.borrowBytes(size);
	}
}
