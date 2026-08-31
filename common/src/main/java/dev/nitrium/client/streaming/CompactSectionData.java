package dev.nitrium.client.streaming;

/**
 * Lock-free section snapshot for off-thread meshing.
 * <p>
 * Contains only palette indices and block-state registry IDs — no {@code WorldChunk},
 * {@code BlockEntity}, or live world references. This is the handoff point between
 * the network/disk engine and the mesh builder.
 */
public final class CompactSectionData {
	public static final int BLOCKS_PER_SECTION = 16 * 16 * 16;

	private final int sectionY;
	private final int[] palette;
	private final byte[] blockIndices;

	public CompactSectionData(int sectionY, int[] palette, byte[] blockIndices) {
		if (palette.length > 256) {
			throw new IllegalArgumentException("palette limited to 256 entries (got " + palette.length + ")");
		}
		if (blockIndices.length != BLOCKS_PER_SECTION) {
			throw new IllegalArgumentException("Expected " + BLOCKS_PER_SECTION + " block indices");
		}

		this.sectionY = sectionY;
		this.palette = palette.clone();
		this.blockIndices = blockIndices.clone();
	}

	public int sectionY() {
		return sectionY;
	}

	public int[] palette() {
		return palette.clone();
	}

	public byte[] blockIndices() {
		return blockIndices.clone();
	}

	public int blockStateId(int index) {
		int paletteIndex = blockIndices[index] & 0xFF;
		return palette[paletteIndex];
	}
}
