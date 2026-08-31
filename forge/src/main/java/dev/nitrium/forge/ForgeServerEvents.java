package dev.nitrium.forge;

import dev.nitrium.platform.ServerEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Binds {@link ServerEvents} to the Forge game event bus.
 */
public final class ForgeServerEvents implements ServerEvents {
	@Override
	public void serverTickStart(Consumer<MinecraftServer> callback) {
		MinecraftForge.EVENT_BUS.addListener((TickEvent.ServerTickEvent.Pre event) -> callback.accept(event.getServer()));
	}

	@Override
	public void serverTickEnd(Consumer<MinecraftServer> callback) {
		MinecraftForge.EVENT_BUS.addListener((TickEvent.ServerTickEvent.Post event) -> callback.accept(event.getServer()));
	}

	@Override
	public void serverWorldTickStart(Consumer<ServerLevel> callback) {
		MinecraftForge.EVENT_BUS.addListener((TickEvent.LevelTickEvent.Pre event) -> {
			if (event.level instanceof ServerLevel level) {
				callback.accept(level);
			}
		});
	}

	@Override
	public void serverStopping(Consumer<MinecraftServer> callback) {
		MinecraftForge.EVENT_BUS.addListener((ServerStoppingEvent event) -> callback.accept(event.getServer()));
	}

	@Override
	public void serverWorldUnload(BiConsumer<MinecraftServer, ServerLevel> callback) {
		MinecraftForge.EVENT_BUS.addListener((LevelEvent.Unload event) -> {
			if (event.getLevel() instanceof ServerLevel level) {
				callback.accept(level.getServer(), level);
			}
		});
	}
}
