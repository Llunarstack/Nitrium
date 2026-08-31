package dev.nitrium.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.nitrium.NitriumMod;
import dev.nitrium.platform.Platform;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class NitriumConfigManager {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static NitriumConfig config;
	private static Path configPath;

	private NitriumConfigManager() {
	}

	public static NitriumConfig get() {
		if (config == null) {
			load();
		}
		return config;
	}

	public static Path path() {
		if (configPath == null) {
			configPath = Platform.configDir().resolve(NitriumMod.MOD_ID + ".json");
		}
		return configPath;
	}

	public static void load() {
		configPath = path();
		if (Files.exists(configPath)) {
			try (Reader reader = Files.newBufferedReader(configPath)) {
				config = GSON.fromJson(reader, NitriumConfig.class);
				if (config == null) {
					config = new NitriumConfig();
				}
			} catch (IOException exception) {
				NitriumMod.LOGGER.warn("Failed to read Nitrium config, using defaults", exception);
				config = new NitriumConfig();
			}
		} else {
			config = new NitriumConfig();
			save();
		}
	}

	public static void save() {
		try {
			Files.createDirectories(path().getParent());
			try (Writer writer = Files.newBufferedWriter(path())) {
				GSON.toJson(config, writer);
			}
		} catch (IOException exception) {
			NitriumMod.LOGGER.warn("Failed to write Nitrium config", exception);
		}
	}
}
