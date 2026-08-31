package dev.nitrium.nativecore;

import dev.nitrium.Nitrium;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Loads {@code nitrium_native} from the system path or extracts it from the mod JAR.
 */
public final class NitriumNativeLoader {
	private static boolean loaded;
	private static boolean available;

	private NitriumNativeLoader() {
	}

	public static boolean load() {
		if (loaded) {
			return available;
		}

		loaded = true;
		try {
			System.loadLibrary("nitrium_native");
			available = true;
			Nitrium.LOGGER.info("Nitrium native core loaded from system library path");
		} catch (UnsatisfiedLinkError systemPathFailure) {
			available = extractAndLoad();
		}

		if (available) {
			Nitrium.LOGGER.info("Nitrium native core {} (AVX2={})", NitriumNative.version(), NitriumNative.hasAvx2());
		} else {
			Nitrium.LOGGER.warn("Nitrium native core unavailable — running Java fallbacks");
		}

		return available;
	}

	public static boolean isAvailable() {
		return available;
	}

	private static boolean extractAndLoad() {
		String os = System.getProperty("os.name", "").toLowerCase();
		String arch = System.getProperty("os.arch", "").toLowerCase();

		String libName;
		String resourcePath;
		if (os.contains("win")) {
			libName = "nitrium_native.dll";
			resourcePath = "native/windows-amd64/nitrium_native.dll";
		} else if (os.contains("linux")) {
			libName = "libnitrium_native.so";
			resourcePath = "native/linux-amd64/libnitrium_native.so";
		} else if (os.contains("mac")) {
			libName = "libnitrium_native.dylib";
			resourcePath = "native/macos-amd64/libnitrium_native.dylib";
		} else {
			return false;
		}

		if (!arch.contains("64")) {
			return false;
		}

		try (InputStream input = NitriumNativeLoader.class.getClassLoader().getResourceAsStream(resourcePath)) {
			if (input == null) {
				return false;
			}

			Path tempDir = Files.createTempDirectory("nitrium-native");
			tempDir.toFile().deleteOnExit();
			Path lib = tempDir.resolve(libName);
			Files.copy(input, lib, StandardCopyOption.REPLACE_EXISTING);
			System.load(lib.toAbsolutePath().toString());
			return true;
		} catch (IOException | UnsatisfiedLinkError exception) {
			Nitrium.LOGGER.debug("Failed to extract native library", exception);
			return false;
		}
	}
}
