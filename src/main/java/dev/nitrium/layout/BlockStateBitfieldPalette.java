package dev.nitrium.layout;

/**
 * Packs block state properties into a single 32-bit integer.
 * Bits 0-15: palette ID, 16-19: facing, 20-23: powered/waterlogged flags, 24-31: reserved.
 */
public final class BlockStateBitfieldPalette {
	private BlockStateBitfieldPalette() {
	}

	public static int pack(int paletteId, int facing, int flags) {
		return (paletteId & 0xFFFF)
				| ((facing & 0x0F) << 16)
				| ((flags & 0x0F) << 20);
	}

	public static int paletteId(int packed) {
		return packed & 0xFFFF;
	}

	public static int facing(int packed) {
		return (packed >> 16) & 0x0F;
	}

	public static int flags(int packed) {
		return (packed >> 20) & 0x0F;
	}
}
