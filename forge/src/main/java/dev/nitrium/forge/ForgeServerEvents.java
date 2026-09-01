package dev.nitrium.forge;

import dev.nitrium.platform.ServerEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Binds {@link ServerEvents} to the Forge game event bus (EventBus 7).
 */
public final class ForgeServerEvents implements ServerEvents {
	@Override
	public void serverTickStart(Consumer<MinecraftServer> callback) {
		TickEvent.ServerTickEvent.Pre.BUS.addListener(
				(TickEvent.ServerTickEvent.Pre event) -> callback.accept(event.server()));
	}

	@Override
	public void serverTickEnd(Consumer<MinecraftServer> callback) {
		TickEvent.ServerTickEvent.Post.BUS.addListener(
				(TickEvent.ServerTickEvent.Post event) -> callback.accept(event.server()));
	}

	@Override
	public void serverWorldTickStart(Consumer<ServerLevel> callback) {
		TickEvent.LevelTickEvent.Pre.BUS.addListener((TickEvent.LevelTickEvent.Pre event) -> {
			if (event.level() instanceof ServerLevel level) {
				callback.accept(level);
			}
		});
	}

	@Override
	public void serverStopping(Consumer<MinecraftServer> callback) {
		ServerStoppingEvent.BUS.addListener((ServerStoppingEvent event) -> callback.accept(event.getServer()));
	}

	@Override
	public void serverWorldUnload(BiConsumer<MinecraftServer, ServerLevel> callback) {
		LevelEvent.Unload.BUS.addListener((LevelEvent.Unload event) -> {
			if (event.getLevel() instanceof ServerLevel level) {
				callback.accept(level.getServer(), level);
			}
		});
	}
}
