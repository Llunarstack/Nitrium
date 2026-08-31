package dev.nitrium.client.streaming;

import dev.nitrium.NitriumMod;
import dev.nitrium.config.NitriumConfigManager;
import dev.nitrium.memory.NitriumWorkerThreads;
import dev.nitrium.platform.Platform;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Async reader/writer for compact section blobs on local disk.
 * Layout: {@code .minecraft/nitrium/cache/<dimension>/<chunkX>/<chunkZ>/<sectionY>.nitsec}
 */
public final class AsyncChunkCacheStore {
	private static AsyncChunkCacheStore instance;

	private final Path cacheRoot;
	private final ExecutorService ioExecutor;

	private AsyncChunkCacheStore(Path cacheRoot) {
		this.cacheRoot = cacheRoot;
		this.ioExecutor = NitriumWorkerThreads.createPool(
				Math.max(1, NitriumConfigManager.get().maxConcurrentCacheReads),
				"nitrium-cache-io"
		);
	}

	public static void init() {
		if (instance != null) {
			return;
		}

		Path root = Platform.gameDir().resolve("nitrium").resolve("cache");
		instance = new AsyncChunkCacheStore(root);
		NitriumMod.LOGGER.info("Nitrium section cache root: {}", root.toAbsolutePath());
	}

	public static AsyncChunkCacheStore get() {
		return instance;
	}

	public CompletableFuture<Optional<CompactSectionData>> readAsync(SectionKey key) {
		return CompletableFuture.supplyAsync(() -> {
			Path path = pathFor(key);
			if (!Files.exists(path)) {
				return Optional.empty();
			}

			try (InputStream input = Files.newInputStream(path)) {
				return Optional.of(ChunkCacheCodec.decode(input));
			} catch (IOException exception) {
				NitriumMod.LOGGER.warn("Failed to read cached section {}", key, exception);
				return Optional.empty();
			}
		}, ioExecutor);
	}

	public CompletableFuture<Void> writeAsync(SectionKey key, CompactSectionData data) {
		return CompletableFuture.runAsync(() -> {
			Path path = pathFor(key);
			try {
				Files.createDirectories(path.getParent());
				try (OutputStream output = Files.newOutputStream(path)) {
					ChunkCacheCodec.encode(data, output);
				}
			} catch (IOException exception) {
				NitriumMod.LOGGER.warn("Failed to write cached section {}", key, exception);
			}
		}, ioExecutor);
	}

	public boolean exists(SectionKey key) {
		return Files.exists(pathFor(key));
	}

	private Path pathFor(SectionKey key) {
		String dimensionPath = key.dimension().toString().replace(':', '_');
		return cacheRoot
				.resolve(dimensionPath)
				.resolve(Integer.toString(key.chunkX()))
				.resolve(Integer.toString(key.chunkZ()))
				.resolve(key.sectionY() + ".nitsec");
	}

	public void shutdown() {
		ioExecutor.shutdownNow();
	}
}
