package dev.nitrium.nativecore;

import dev.nitrium.config.NitriumConfigManager;

import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * Packet compression with a Java Deflater/Inflater fallback. The native path is currently an
 * identity wrapper — swapping in libdeflate/zstd (SIMD) is still to do.
 */
public final class NativePacketCompressor {
	private NativePacketCompressor() {
	}

	public static byte[] compress(byte[] input) {
		if (NitriumConfigManager.get().enableNativePacketCompression && NitriumNativeLoader.isAvailable()) {
			byte[] output = new byte[input.length + 16];
			int written = nativeCompress(input, output);
			if (written > 0) {
				byte[] result = new byte[written];
				System.arraycopy(output, 0, result, 0, written);
				return result;
			}
		}
		return javaCompress(input);
	}

	public static byte[] decompress(byte[] input) {
		if (NitriumConfigManager.get().enableNativePacketCompression && NitriumNativeLoader.isAvailable()) {
			byte[] output = new byte[input.length * 4];
			int written = nativeDecompress(input, output);
			if (written > 0) {
				byte[] result = new byte[written];
				System.arraycopy(output, 0, result, 0, written);
				return result;
			}
		}
		return javaDecompress(input);
	}

	private static byte[] javaCompress(byte[] input) {
		Deflater deflater = new Deflater();
		deflater.setInput(input);
		deflater.finish();
		byte[] buffer = new byte[input.length + 64];
		int length = deflater.deflate(buffer);
		deflater.end();
		byte[] result = new byte[length];
		System.arraycopy(buffer, 0, result, 0, length);
		return result;
	}

	private static byte[] javaDecompress(byte[] input) {
		Inflater inflater = new Inflater();
		inflater.setInput(input);
		byte[] buffer = new byte[input.length * 4];
		try {
			int length = inflater.inflate(buffer);
			byte[] result = new byte[length];
			System.arraycopy(buffer, 0, result, 0, length);
			return result;
		} catch (Exception exception) {
			return input;
		} finally {
			inflater.end();
		}
	}

	private static native int nativeCompress(byte[] input, byte[] output);

	private static native int nativeDecompress(byte[] input, byte[] output);
}
