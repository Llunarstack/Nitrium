package dev.nitrium.storage;

import net.minecraft.world.level.ChunkPos;

import java.nio.ByteBuffer;

/**
 * Encoded chunk save record: chunk X/Z, payload length, raw bytes.
 */
public record ChunkSavePayload(long chunkX, long chunkZ, byte[] data) {
	public static ChunkSavePayload encode(ChunkPos pos, byte[] payload) {
		return new ChunkSavePayload(pos.x, pos.z, payload);
	}

	public byte[] encoded() {
		ByteBuffer buffer = ByteBuffer.allocate(20 + data.length);
		buffer.putLong(chunkX);
		buffer.putLong(chunkZ);
		buffer.putInt(data.length);
		buffer.put(data);
		return buffer.array();
	}

	public static ChunkSavePayload decode(byte[] encoded) {
		ByteBuffer buffer = ByteBuffer.wrap(encoded);
		long x = buffer.getLong();
		long z = buffer.getLong();
		int length = buffer.getInt();
		byte[] data = new byte[length];
		buffer.get(data);
		return new ChunkSavePayload(x, z, data);
	}
}
