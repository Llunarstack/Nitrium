package dev.nitrium.fabric;

import com.mojang.brigadier.CommandDispatcher;
import dev.nitrium.platform.ServerEvents;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Binds {@link ServerEvents} to the Fabric API lifecycle/tick events.
 */
public final class FabricServerEvents implements ServerEvents {
	@Override
	public void serverTickStart(Consumer<MinecraftServer> callback) {
		ServerTickEvents.START_SERVER_TICK.register(callback::accept);
	}

	@Override
	public void serverTickEnd(Consumer<MinecraftServer> callback) {
		ServerTickEvents.END_SERVER_TICK.register(callback::accept);
	}

	@Override
	public void serverWorldTickStart(Consumer<ServerLevel> callback) {
		ServerTickEvents.START_WORLD_TICK.register(callback::accept);
	}

	@Override
	public void serverStopping(Consumer<MinecraftServer> callback) {
		ServerLifecycleEvents.SERVER_STOPPING.register(callback::accept);
	}

	@Override
	public void serverWorldUnload(BiConsumer<MinecraftServer, ServerLevel> callback) {
		ServerWorldEvents.UNLOAD.register(callback::accept);
	}

	@Override
	public void registerCommands(Consumer<CommandDispatcher<CommandSourceStack>> callback) {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> callback.accept(dispatcher));
	}
}
