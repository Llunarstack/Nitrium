package dev.nitrium.client.streaming;

import dev.nitrium.Nitrium;
import dev.nitrium.config.NitriumConfig;
import dev.nitrium.config.NitriumConfigManager;
import dev.nitrium.client.culling.CullingPipeline;
import dev.nitrium.client.platform.ClientEvents;
import dev.nitrium.client.streaming.ChunkCacheCodec;
import dev.nitrium.client.streaming.GeometryBufferPool;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Drives disk-cache reads and mesh-task scheduling for extended render distance. Right now it only
 * decodes sections asynchronously; nothing is submitted to Sodium for drawing yet.
 */
public final class StreamingChunkLoader {
	private static StreamingChunkLoader instance;

	private final MeshTaskScheduler meshTasks = new MeshTaskScheduler();
	private double lastCameraX;
	private double lastCameraY;
	private double lastCameraZ;
	private double velocityX;
	private double velocityY;
	private double velocityZ;
	private boolean cameraInitialized;

	private StreamingChunkLoader() {
	}

	public static void init() {
		if (instance != null) {
			return;
		}

		instance = new StreamingChunkLoader();
		instance.register();
	}

	private void register() {
		ClientEvents.get().clientTickEnd(this::onClientTick);
		Nitrium.LOGGER.info("Nitrium streaming loader active");
	}

	private void onClientTick(Minecraft client) {
		NitriumConfig config = NitriumConfigManager.get();
		if (!config.enableSectionDiskCache) {
			return;
		}

		LocalPlayer player = client.player;
		if (player == null || client.level == null) {
			return;
		}

		updateCameraVelocity(player);

		// Tasks are dequeued but not yet built into GPU geometry.
		meshTasks.drainUpTo(config.maxConcurrentMeshTasks, task -> {
			try {
				java.io.ByteArrayOutputStream stream = new java.io.ByteArrayOutputStream();
				ChunkCacheCodec.encode(task.data(), stream);
				byte[] bytes = stream.toByteArray();
				int offset = GeometryBufferPool.get().tryReserve(bytes.length);
				if (offset >= 0) {
					GeometryBufferPool.get().writeSlice(offset, java.nio.ByteBuffer.wrap(bytes));
				}
			} catch (java.io.IOException exception) {
				Nitrium.LOGGER.warn("Failed to encode section mesh for {}", task.key(), exception);
			}
		});
	}

	private void updateCameraVelocity(LocalPlayer player) {
		double x = player.getX();
		double y = player.getY();
		double z = player.getZ();

		if (cameraInitialized) {
			velocityX = x - lastCameraX;
			velocityY = y - lastCameraY;
			velocityZ = z - lastCameraZ;
		} else {
			velocityX = velocityY = velocityZ = 0.0;
			cameraInitialized = true;
		}

		lastCameraX = x;
		lastCameraY = y;
		lastCameraZ = z;
	}

	/**
	 * Request a section from disk cache and enqueue it for meshing if present.
	 */
	public CompletableFuture<Boolean> requestCachedSection(SectionKey key) {
		return AsyncChunkCacheStore.get()
				.readAsync(key)
				.thenApply(optional -> {
					if (optional.isEmpty()) {
						return false;
					}

					CullingPipeline pipeline = CullingPipeline.get();
					if (pipeline != null) {
						Minecraft client = Minecraft.getInstance();
						if (client.levelRenderer != null) {
							var frustum = client.levelRenderer.getCapturedFrustum();
							if (frustum != null) {
								pipeline.submitSection(key, frustum);
							}
						}
					}

					double priority = MeshTaskPriority.compute(
							lastCameraX,
							lastCameraY,
							lastCameraZ,
							velocityX,
							velocityY,
							velocityZ,
							key
					);
					meshTasks.enqueue(key, optional.get(), priority);
					return true;
				});
	}

	/**
	 * Persist a section snapshot captured from the live world (called from capture hooks).
	 */
	public CompletableFuture<Void> storeSection(SectionKey key, CompactSectionData data) {
		NitriumConfig config = NitriumConfigManager.get();
		if (!config.enableSectionDiskCache) {
			return CompletableFuture.completedFuture(null);
		}

		double priority = MeshTaskPriority.compute(
				lastCameraX,
				lastCameraY,
				lastCameraZ,
				velocityX,
				velocityY,
				velocityZ,
				key
		);
		meshTasks.enqueue(key, data, priority);
		return AsyncChunkCacheStore.get().writeAsync(key, data);
	}

	public static StreamingChunkLoader get() {
		return instance;
	}

	public MeshTaskScheduler meshTasks() {
		return meshTasks;
	}

	public void onWorldUnload() {
		meshTasks.drainUpTo(Integer.MAX_VALUE, task -> {});
	}

	public static SectionKey key(Identifier dimension, int chunkX, int sectionY, int chunkZ) {
		return new SectionKey(dimension, chunkX, sectionY, chunkZ);
	}

	public Optional<CompactSectionData> readBlocking(SectionKey key) {
		try {
			return AsyncChunkCacheStore.get().readAsync(key).get();
		} catch (Exception exception) {
			Nitrium.LOGGER.warn("Blocking cache read failed for {}", key, exception);
			return Optional.empty();
		}
	}
}
