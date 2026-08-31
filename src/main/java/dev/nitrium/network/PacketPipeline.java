package dev.nitrium.network;

import dev.nitrium.NitriumMod;
import dev.nitrium.config.NitriumConfigManager;
import dev.nitrium.memory.NitriumWorkerThreads;
import dev.nitrium.nativecore.NativePacketCompressor;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Off-thread packet encode/decode pipeline with zero-copy buffers.
 */
public final class PacketPipeline {
	private static PacketPipeline instance;

	private final ExecutorService workers;
	private final AtomicLong packetsCompressed = new AtomicLong();
	private final AtomicLong packetsDecompressed = new AtomicLong();
	private final AtomicLong bytesProcessed = new AtomicLong();

	private PacketPipeline() {
		int threads = Math.max(1, NitriumConfigManager.get().networkWorkerThreads);
		this.workers = NitriumWorkerThreads.createPool(threads, "NitriumNet");
	}

	public static void init() {
		if (instance != null) {
			return;
		}
		instance = new PacketPipeline();
		NitriumMod.LOGGER.info("Nitrium network pipeline active (workers={})",
				NitriumConfigManager.get().networkWorkerThreads);
	}

	public CompletableFuture<byte[]> compressAsync(byte[] payload) {
		return CompletableFuture.supplyAsync(() -> {
			byte[] compressed = NativePacketCompressor.compress(payload);
			packetsCompressed.incrementAndGet();
			bytesProcessed.addAndGet(payload.length);
			return compressed;
		}, workers);
	}

	public CompletableFuture<byte[]> decompressAsync(byte[] payload) {
		return CompletableFuture.supplyAsync(() -> {
			byte[] decompressed = NativePacketCompressor.decompress(payload);
			packetsDecompressed.incrementAndGet();
			bytesProcessed.addAndGet(payload.length);
			return decompressed;
		}, workers);
	}

	public long packetsCompressed() {
		return packetsCompressed.get();
	}

	public long packetsDecompressed() {
		return packetsDecompressed.get();
	}

	public long bytesProcessed() {
		return bytesProcessed.get();
	}

	public static PacketPipeline get() {
		return instance;
	}

	public void shutdown() {
		workers.shutdown();
		try {
			if (!workers.awaitTermination(5, TimeUnit.SECONDS)) {
				workers.shutdownNow();
			}
		} catch (InterruptedException exception) {
			workers.shutdownNow();
			Thread.currentThread().interrupt();
		}
		instance = null;
	}
}
