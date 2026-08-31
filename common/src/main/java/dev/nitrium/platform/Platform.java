package dev.nitrium.platform;

import java.nio.file.Path;
import java.util.Optional;
import java.util.ServiceLoader;

/**
 * Loader-agnostic access to the running environment (mod list, directories). Each loader ships a
 * {@link Provider} implementation discovered via {@link ServiceLoader} (a
 * {@code META-INF/services} entry), so this resolves even during mixin plugin evaluation — before
 * any mod entrypoint runs. This is the seam that lets the same code run on Fabric, NeoForge, Forge
 * and Quilt.
 */
public final class Platform {
	public interface Provider {
		boolean isModLoaded(String id);

		Path configDir();

		Path gameDir();

		Optional<String> modName(String id);
	}

	private static Provider provider;

	private Platform() {
	}

	/** Override the auto-discovered provider (mainly for tests). */
	public static void install(Provider p) {
		provider = p;
	}

	private static Provider provider() {
		if (provider == null) {
			provider = ServiceLoader.load(Provider.class, Platform.class.getClassLoader())
					.findFirst()
					.orElse(null);
		}
		return provider;
	}

	public static boolean isModLoaded(String id) {
		Provider p = provider();
		return p != null && p.isModLoaded(id);
	}

	public static Path configDir() {
		return provider().configDir();
	}

	public static Path gameDir() {
		return provider().gameDir();
	}

	public static Optional<String> modName(String id) {
		Provider p = provider();
		return p != null ? p.modName(id) : Optional.empty();
	}
}
