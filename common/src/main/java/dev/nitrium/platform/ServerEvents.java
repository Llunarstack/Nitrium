package dev.nitrium.platform;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Loader-agnostic server-side event hooks. The active loader installs an implementation; common
 * engines register their callbacks through {@link #get()}. On Fabric these map to
 * {@code ServerTickEvents} / {@code ServerLifecycleEvents}; on NeoForge/Forge to the game event bus.
 */
public interface ServerEvents {
	void serverTickStart(Consumer<MinecraftServer> callback);

	void serverTickEnd(Consumer<MinecraftServer> callback);

	void serverWorldTickStart(Consumer<ServerLevel> callback);

	void serverStopping(Consumer<MinecraftServer> callback);

	void serverWorldUnload(BiConsumer<MinecraftServer, ServerLevel> callback);

	void registerCommands(Consumer<CommandDispatcher<CommandSourceStack>> callback);

	static ServerEvents get() {
		return Holder.instance;
	}

	static void install(ServerEvents events) {
		Holder.instance = events;
	}

	final class Holder {
		private static ServerEvents instance;

		private Holder() {
		}
	}
}
