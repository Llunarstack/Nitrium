package dev.nitrium.nativecore;

/**
 * JNI entry points into the Nitrium C++20 native core.
 */
public final class NitriumNative {
	private NitriumNative() {
	}

	public static native String nitriumVersion();

	public static native boolean hasAvx2();

	public static String version() {
		if (!NitriumNativeLoader.isAvailable()) {
			return "java-fallback";
		}
		return nitriumVersion();
	}
}
