package dev.nitrium.memory;

import dev.nitrium.NitriumMod;

import java.lang.ref.Cleaner;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Central {@link Cleaner} for off-heap and native resources.
 * Registers fallback native {@code free()} when wrappers are GC'd without {@code close()}.
 */
public final class NativeResourceCleaner {
	private static final Cleaner CLEANER = Cleaner.create();
	private static final AtomicLong registered = new AtomicLong();
	private static final AtomicLong cleaned = new AtomicLong();

	private NativeResourceCleaner() {
	}

	public static Cleaner.Cleanable register(Object owner, Runnable cleanup) {
		registered.incrementAndGet();
		return CLEANER.register(owner, () -> {
			try {
				cleanup.run();
				cleaned.incrementAndGet();
			} catch (Exception exception) {
				NitriumMod.LOGGER.warn("Native resource cleanup failed", exception);
			}
		});
	}

	public static long registeredCount() {
		return registered.get();
	}

	public static long cleanedCount() {
		return cleaned.get();
	}
}
