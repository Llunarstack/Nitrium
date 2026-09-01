package dev.nitrium.pregen;

import dev.nitrium.Nitrium;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.status.ChunkStatus;

import java.util.function.Consumer;

/**
 * Incremental chunk pre-generation. One job at a time: given a centre chunk and a radius, it walks
 * the square area a handful of chunks per server tick (so the server keeps ticking) and asks the
 * chunk source to generate each to {@link ChunkStatus#FULL}. Pairs with the LZ4 region codec so the
 * generated chunks are written quickly.
 */
public final class PregenManager {
	private static final PregenManager INSTANCE = new PregenManager();

	private ServerLevel level;
	private int centerX;
	private int centerZ;
	private int radius;
	private int side;
	private int total;
	private int done;
	private int nextIndex;
	private long lastReportMs;
	private Consumer<Component> feedback;
	private volatile boolean active;

	private PregenManager() {
	}

	public static PregenManager get() {
		return INSTANCE;
	}

	public boolean isActive() {
		return active;
	}

	/**
	 * @return {@code false} if a job is already running
	 */
	public synchronized boolean start(ServerLevel level, int centerChunkX, int centerChunkZ, int radiusChunks,
			Consumer<Component> feedback) {
		if (active) {
			return false;
		}
		this.level = level;
		this.centerX = centerChunkX;
		this.centerZ = centerChunkZ;
		this.radius = radiusChunks;
		this.side = radiusChunks * 2 + 1;
		this.total = side * side;
		this.done = 0;
		this.nextIndex = 0;
		this.feedback = feedback;
		this.lastReportMs = 0L;
		this.active = true;
		return true;
	}

	public synchronized void stop() {
		if (!active) {
			return;
		}
		report(Component.literal("Nitrium pre-gen stopped at " + done + "/" + total + " chunks."));
		clear();
	}

	public synchronized String status() {
		if (!active) {
			return "Nitrium pre-gen: idle.";
		}
		return "Nitrium pre-gen: " + done + "/" + total + " chunks (" + percent() + "%) in " + level.dimension().identifier();
	}

	/** Advance the current job by up to {@code chunksPerTick} chunks. Called on the server thread. */
	public synchronized void tick(int chunksPerTick) {
		if (!active || level == null) {
			return;
		}

		int budget = Math.max(1, chunksPerTick);
		for (int i = 0; i < budget && nextIndex < total; i++, nextIndex++) {
			int col = nextIndex % side;
			int row = nextIndex / side;
			int chunkX = centerX - radius + col;
			int chunkZ = centerZ - radius + row;
			try {
				level.getChunk(chunkX, chunkZ, ChunkStatus.FULL, true);
			} catch (Exception exception) {
				Nitrium.LOGGER.warn("Nitrium pre-gen failed for chunk {},{}", chunkX, chunkZ, exception);
			}
			done++;
		}

		long now = System.currentTimeMillis();
		boolean finished = nextIndex >= total;
		if (now - lastReportMs > 3000L || finished) {
			lastReportMs = now;
			report(Component.literal("Nitrium pre-gen: " + done + "/" + total + " chunks (" + percent() + "%)"));
		}

		if (finished) {
			report(Component.literal("Nitrium pre-gen complete: " + total + " chunks."));
			clear();
		}
	}

	private int percent() {
		return total == 0 ? 100 : (int) ((long) done * 100L / total);
	}

	private void report(Component message) {
		if (feedback != null) {
			try {
				feedback.accept(message);
			} catch (Exception ignored) {
				// Command source may have gone away (player disconnected); ignore.
			}
		}
	}

	private void clear() {
		active = false;
		level = null;
		feedback = null;
	}
}
