package dev.nitrium.storage;

import dev.nitrium.Nitrium;
import dev.nitrium.platform.Platform;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Writes async chunk payloads to {@code .nitrium/chunk-queue/} under the world directory.
 */
public final class ChunkDiskWriter {
	private static Path queueDir;

	private ChunkDiskWriter() {
	}

	public static void init(Path worldDirectory) {
		queueDir = worldDirectory.resolve(".nitrium").resolve("chunk-queue");
		try {
			Files.createDirectories(queueDir);
		} catch (IOException exception) {
			Nitrium.LOGGER.warn("Failed to create Nitrium chunk queue directory", exception);
		}
	}

	public static void write(ChunkSavePayload payload) {
		writeEncoded(payload.encoded());
	}

	public static void writeEncoded(byte[] encoded) {
		if (queueDir == null) {
			queueDir = Platform.configDir().resolve("chunk-queue");
			try {
				Files.createDirectories(queueDir);
			} catch (IOException exception) {
				Nitrium.LOGGER.warn("Failed to create fallback chunk queue directory", exception);
				return;
			}
		}

		ChunkSavePayload payload = ChunkSavePayload.decode(encoded);
		Path file = queueDir.resolve(payload.chunkX() + "_" + payload.chunkZ() + "_" + System.nanoTime() + ".chunk");
		try (FileChannel channel = FileChannel.open(file, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
			ByteBuffer buffer = ByteBuffer.wrap(encoded);
			channel.write(buffer);
		} catch (IOException exception) {
			Nitrium.LOGGER.warn("Failed to write async chunk payload for {},{}", payload.chunkX(), payload.chunkZ(), exception);
		}
	}
}
