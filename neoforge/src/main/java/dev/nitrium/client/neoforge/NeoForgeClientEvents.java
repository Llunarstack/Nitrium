package dev.nitrium.client.neoforge;

import dev.nitrium.client.platform.ClientEvents;
import dev.nitrium.client.platform.ClientRenderStages;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.LevelEvent;

import java.util.function.Consumer;

/**
 * Binds {@link ClientEvents} to the NeoForge game event bus. World-render stages map onto
 * {@link RenderLevelStageEvent}; the HUD is drawn from {@link RenderGuiEvent.Post} so it can be
 * registered any time (unlike mod-bus GUI layers). {@code clientStopping} has no NeoForge event and
 * is a no-op — the JVM reclaims GL resources on exit.
 */
public final class NeoForgeClientEvents implements ClientEvents {
	@Override
	public void clientTickStart(Consumer<net.minecraft.client.Minecraft> callback) {
		NeoForge.EVENT_BUS.addListener((ClientTickEvent.Pre event) ->
				callback.accept(net.minecraft.client.Minecraft.getInstance()));
	}

	@Override
	public void clientTickEnd(Consumer<net.minecraft.client.Minecraft> callback) {
		NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) ->
				callback.accept(net.minecraft.client.Minecraft.getInstance()));
	}

	// NeoForge 21.11 exposes each render stage as its own event subclass.
	@Override
	public void worldRenderStart(Runnable callback) {
		ClientRenderStages.onRenderStart(callback);
	}

	@Override
	public void worldRenderBeforeEntities(Runnable callback) {
		ClientRenderStages.onBeforeEntities(callback);
	}

	@Override
	public void worldRenderAfterEntities(Runnable callback) {
		ClientRenderStages.onAfterEntities(callback);
	}

	@Override
	public void worldRenderBeforeDebug(Runnable callback) {
		ClientRenderStages.onBeforeDebug(callback);
	}

	@Override
	public void worldRenderEnd(Runnable callback) {
		ClientRenderStages.onRenderEnd(callback);
	}

	@Override
	public void clientWorldChanged(Consumer<ClientLevel> callback) {
		NeoForge.EVENT_BUS.addListener((LevelEvent.Load event) -> {
			if (event.getLevel() instanceof ClientLevel level) {
				callback.accept(level);
			}
		});
	}

	@Override
	public void clientStopping(Runnable callback) {
		// No NeoForge client-stopping event; GL resources are freed by the OS on exit.
	}

	@Override
	public void hud(Identifier id, HudLayer layer) {
		NeoForge.EVENT_BUS.addListener((RenderGuiEvent.Post event) ->
				layer.render(event.getGuiGraphics(), event.getPartialTick()));
	}
}
