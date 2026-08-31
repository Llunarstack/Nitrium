package dev.nitrium.client.streaming;

import dev.nitrium.NitriumMod;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Nitrium binary section format (v1).
 * <pre>
 * [magic "NTRM"][version u8][sectionY i32][paletteLen u16]
 * [palette: paletteLen × i32 block state registry ids]
 * [indices: 4096 × u8 palette indices]
 * </pre>
 * Run-length encoding and biome/light layers are reserved for v2.
 */
public final class ChunkCacheCodec {
	public static final int FORMAT_VERSION = 1;
	private static final byte[] MAGIC = "NTRM".getBytes(StandardCharsets.US_ASCII);

	private ChunkCacheCodec() {
	}

	public static void encode(CompactSectionData section, OutputStream output) throws IOException {
		DataOutputStream out = new DataOutputStream(output);
		out.write(MAGIC);
		out.writeByte(FORMAT_VERSION);
		out.writeInt(section.sectionY());
		out.writeShort(section.palette().length);

		for (int blockStateId : section.palette()) {
			out.writeInt(blockStateId);
		}

		out.write(section.blockIndices());
	}

	public static CompactSectionData decode(InputStream input) throws IOException {
		DataInputStream in = new DataInputStream(input);

		byte[] magic = in.readNBytes(MAGIC.length);
		if (!java.util.Arrays.equals(magic, MAGIC)) {
			throw new IOException("Invalid Nitrium section magic");
		}

		int version = in.readUnsignedByte();
		if (version != FORMAT_VERSION) {
			throw new IOException("Unsupported Nitrium section version: " + version);
		}

		int sectionY = in.readInt();
		int paletteLen = in.readUnsignedShort();
		if (paletteLen > 256) {
			throw new IOException("Palette too large for v1: " + paletteLen);
		}

		int[] palette = new int[paletteLen];
		for (int i = 0; i < paletteLen; i++) {
			palette[i] = in.readInt();
		}

		byte[] indices = in.readNBytes(CompactSectionData.BLOCKS_PER_SECTION);
		if (indices.length != CompactSectionData.BLOCKS_PER_SECTION) {
			throw new IOException("Truncated block index payload");
		}

		return new CompactSectionData(sectionY, palette, indices);
	}
}
