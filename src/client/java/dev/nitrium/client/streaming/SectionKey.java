package dev.nitrium.client.streaming;

import net.minecraft.resources.Identifier;

/**
 * Immutable key for a single 16³ section within a dimension.
 */
public record SectionKey(Identifier dimension, int chunkX, int sectionY, int chunkZ) {
	private static final int SECTION_SIZE = 16;

	public int blockX() {
		return chunkX << 4;
	}

	public int blockY() {
		return sectionY << 4;
	}

	public int blockZ() {
		return chunkZ << 4;
	}

	public double centerX() {
		return blockX() + SECTION_SIZE * 0.5;
	}

	public double centerY() {
		return blockY() + SECTION_SIZE * 0.5;
	}

	public double centerZ() {
		return blockZ() + SECTION_SIZE * 0.5;
	}

	public long chunkLongKey() {
		return ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
	}
}
