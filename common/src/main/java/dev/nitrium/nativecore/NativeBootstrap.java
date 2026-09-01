package dev.nitrium.nativecore;

import dev.nitrium.Nitrium;
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
			Nitrium.LOGGER.info("Nitrium native core disabled via config");
			return;
		}

		NitriumNativeLoader.load();
		CpuCapabilities.probe();
		memoryArena = NativeMemoryArena.create();
		Nitrium.LOGGER.info("Nitrium native bootstrap complete (available={})", NitriumNativeLoader.isAvailable());
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
