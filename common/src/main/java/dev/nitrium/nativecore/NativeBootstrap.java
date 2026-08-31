package dev.nitrium.nativecore;

import dev.nitrium.NitriumMod;
import dev.nitrium.config.NitriumConfigManager;

/**
 * Initializes Nitrium's native core (JNI / off-heap / SIMD).
 */
public final class NativeBootstrap {
	private static NativeMemoryArena memoryArena;

	private NativeBootstrap() {
	}

	public static void init() {
		if (!NitriumConfigManager.get().enableNativeCore) {
			NitriumMod.LOGGER.info("Nitrium native core disabled via config");
			return;
		}

		NitriumNativeLoader.load();
		memoryArena = NativeMemoryArena.create();
		NitriumMod.LOGGER.info("Nitrium native bootstrap complete (available={})", NitriumNativeLoader.isAvailable());
	}

	public static NativeMemoryArena memoryArena() {
		return memoryArena;
	}

	public static void shutdown() {
		if (memoryArena != null) {
			memoryArena.close();
			memoryArena = null;
		}
	}
}
