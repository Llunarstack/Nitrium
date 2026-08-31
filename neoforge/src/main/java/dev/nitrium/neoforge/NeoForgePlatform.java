package dev.nitrium.neoforge;

import dev.nitrium.platform.Platform;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.file.Path;
import java.util.Optional;

/**
 * NeoForge implementation of {@link Platform.Provider}.
 */
public final class NeoForgePlatform implements Platform.Provider {
	@Override
	public boolean isModLoaded(String id) {
		return ModList.get() != null && ModList.get().isLoaded(id);
	}

	@Override
	public Path configDir() {
		return FMLPaths.CONFIGDIR.get();
	}

	@Override
	public Path gameDir() {
		return FMLPaths.GAMEDIR.get();
	}

	@Override
	public Optional<String> modName(String id) {
		return ModList.get().getModContainerById(id).map(container -> container.getModInfo().getDisplayName());
	}
}
