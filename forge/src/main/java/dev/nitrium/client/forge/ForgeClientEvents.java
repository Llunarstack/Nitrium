package dev.nitrium.client.forge;

import dev.nitrium.client.platform.ClientEvents;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.Identifier;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;

import java.util.function.Consumer;

/**
 * Binds {@link ClientEvents} to the Forge game event bus.
 *
 * <p>Forge 61.x reworked the render pipeline and no longer exposes discrete world-render stage
 * events, so the world-render hooks are no-ops here — the client features that used them are all
 * still unimplemented stubs, and the tick-driven governor plus the entity-cull mixin (which don't
 * need render events) work regardless. The optional debug HUD is not wired on Forge.
 */
public final class ForgeClientEvents implements ClientEvents {
	@Override
	public void clientTickStart(Consumer<net.minecraft.client.Minecraft> callback) {
		MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent.Pre event) ->
				callback.accept(net.minecraft.client.Minecraft.getInstance()));
	}

	@Override
	public void clientTickEnd(Consumer<net.minecraft.client.Minecraft> callback) {
		MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent.Post event) ->
				callback.accept(net.minecraft.client.Minecraft.getInstance()));
	}

	@Override
	public void worldRenderStart(Runnable callback) {
		// No Forge 61.x equivalent.
	}

	@Override
	public void worldRenderBeforeEntities(Runnable callback) {
		// No Forge 61.x equivalent.
	}

	@Override
	public void worldRenderAfterEntities(Runnable callback) {
		// No Forge 61.x equivalent.
	}

	@Override
	public void worldRenderBeforeDebug(Runnable callback) {
		// No Forge 61.x equivalent.
	}

	@Override
	public void worldRenderEnd(Runnable callback) {
		// No Forge 61.x equivalent.
	}

	@Override
	public void clientWorldChanged(Consumer<ClientLevel> callback) {
		// Flush caches on disconnect; the consumer tracks the previous world itself.
		MinecraftForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> callback.accept(null));
	}

	@Override
	public void clientStopping(Runnable callback) {
		// No Forge client-stopping event; GL resources are freed by the OS on exit.
	}

	@Override
	public void hud(Identifier id, HudLayer layer) {
		// The optional debug overlay is not wired on Forge's reworked GUI layer system.
	}
}
