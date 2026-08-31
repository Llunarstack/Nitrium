package dev.nitrium.memory;

import dev.nitrium.Nitrium;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Background workers with enlarged stacks for deep spatial/grid tasks.
 */
public final class NitriumWorkerThreads {
	private static final int STACK_SIZE_BYTES = 2 * 1024 * 1024;

	private NitriumWorkerThreads() {
	}

	public static ExecutorService createPool(int threads, String namePrefix) {
		AtomicInteger counter = new AtomicInteger();
		ThreadFactory factory = runnable -> {
			Thread thread = new Thread(null, runnable, namePrefix + "-" + counter.incrementAndGet(), STACK_SIZE_BYTES);
			thread.setDaemon(true);
			return thread;
		};

		Nitrium.LOGGER.info("Nitrium worker pool '{}' created ({} threads, {} MB stack)", namePrefix, threads, STACK_SIZE_BYTES / (1024 * 1024));
		return Executors.newFixedThreadPool(Math.max(1, threads), factory);
	}
}
