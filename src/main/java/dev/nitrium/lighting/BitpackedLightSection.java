package dev.nitrium.lighting;

/**
 * Bitpacked skylight (high nibble) + blocklight (low nibble) per block in a section.
 * 4096 blocks × 4 bits each = 2048 bytes per section (vs nested object arrays).
 */
public final class BitpackedLightSection {
	public static final int SECTION_VOLUME = 16 * 16 * 16;

	private final byte[] blockLight = new byte[SECTION_VOLUME];
	private final byte[] skyLight = new byte[SECTION_VOLUME];

	public void setBlockLight(int index, int level) {
		blockLight[index] = (byte) (level & 0x0F);
	}

	public void setSkyLight(int index, int level) {
		skyLight[index] = (byte) (level & 0x0F);
	}

	public int getBlockLight(int index) {
		return blockLight[index] & 0x0F;
	}

	public int getSkyLight(int index) {
		return skyLight[index] & 0x0F;
	}

	public int pack(int index) {
		return (skyLight[index] & 0x0F) << 4 | (blockLight[index] & 0x0F);
	}

	public void unpack(int index, int packed) {
		blockLight[index] = (byte) (packed & 0x0F);
		skyLight[index] = (byte) ((packed >> 4) & 0x0F);
	}

	public byte[] blockLightArray() {
		return blockLight;
	}

	public byte[] skyLightArray() {
		return skyLight;
	}
}
