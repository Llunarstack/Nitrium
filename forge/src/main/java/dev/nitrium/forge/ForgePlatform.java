package dev.nitrium.forge;

import dev.nitrium.platform.Platform;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Forge implementation of {@link Platform.Provider}.
 */
public final class ForgePlatform implements Platform.Provider {
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
