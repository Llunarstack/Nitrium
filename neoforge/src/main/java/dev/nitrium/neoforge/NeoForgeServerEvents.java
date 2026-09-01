package dev.nitrium.neoforge;

import com.mojang.brigadier.CommandDispatcher;
import dev.nitrium.platform.ServerEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Binds {@link ServerEvents} to the NeoForge game event bus.
 */
public final class NeoForgeServerEvents implements ServerEvents {
	@Override
	public void serverTickStart(Consumer<MinecraftServer> callback) {
		NeoForge.EVENT_BUS.addListener((ServerTickEvent.Pre event) -> callback.accept(event.getServer()));
	}

	@Override
	public void serverTickEnd(Consumer<MinecraftServer> callback) {
		NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> callback.accept(event.getServer()));
	}

	@Override
	public void serverWorldTickStart(Consumer<ServerLevel> callback) {
		NeoForge.EVENT_BUS.addListener((LevelTickEvent.Pre event) -> {
			if (event.getLevel() instanceof ServerLevel level) {
				callback.accept(level);
			}
		});
	}

	@Override
	public void serverStopping(Consumer<MinecraftServer> callback) {
		NeoForge.EVENT_BUS.addListener((ServerStoppingEvent event) -> callback.accept(event.getServer()));
	}

	@Override
	public void serverWorldUnload(BiConsumer<MinecraftServer, ServerLevel> callback) {
		NeoForge.EVENT_BUS.addListener((LevelEvent.Unload event) -> {
			if (event.getLevel() instanceof ServerLevel level) {
				callback.accept(level.getServer(), level);
			}
		});
	}

	@Override
	public void registerCommands(Consumer<CommandDispatcher<CommandSourceStack>> callback) {
		NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> callback.accept(event.getDispatcher()));
	}
}
