package dev.nitrium.layout;

/**
 * Structure-of-Arrays layout for a 16×16×16 chunk section.
 * Contiguous primitive arrays eliminate pointer-chasing through BlockState object graphs.
 */
public final class SectionSoALayout {
	public static final int SECTION_VOLUME = 16 * 16 * 16;

	private final int[] blockIds = new int[SECTION_VOLUME];
	private final int[] packedStates = new int[SECTION_VOLUME];
	private final byte[] lightLevels = new byte[SECTION_VOLUME];
	private final byte[] flags = new byte[SECTION_VOLUME];

	public void set(int index, int blockId, int packedState, int light, byte sectionFlags) {
		blockIds[index] = blockId;
		packedStates[index] = packedState;
		lightLevels[index] = (byte) (light & 0xFF);
		flags[index] = sectionFlags;
	}

	public int blockId(int index) {
		return blockIds[index];
	}

	public int packedState(int index) {
		return packedStates[index];
	}

	public int light(int index) {
		return lightLevels[index] & 0xFF;
	}

	public byte flags(int index) {
		return flags[index];
	}

	public int[] blockIdArray() {
		return blockIds;
	}

	public int[] packedStateArray() {
		return packedStates;
	}
}
