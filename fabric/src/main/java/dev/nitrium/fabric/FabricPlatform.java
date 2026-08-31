package dev.nitrium.fabric;

import dev.nitrium.platform.Platform;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Fabric (and Quilt, via its Fabric-compat layer) implementation of {@link Platform.Provider}.
 */
public final class FabricPlatform implements Platform.Provider {
	@Override
	public boolean isModLoaded(String id) {
		return FabricLoader.getInstance().isModLoaded(id);
	}

	@Override
	public Path configDir() {
		return FabricLoader.getInstance().getConfigDir();
	}

	@Override
	public Path gameDir() {
		return FabricLoader.getInstance().getGameDir();
	}

	@Override
	public Optional<String> modName(String id) {
		return FabricLoader.getInstance().getModContainer(id)
				.map(container -> container.getMetadata().getName());
	}
}
