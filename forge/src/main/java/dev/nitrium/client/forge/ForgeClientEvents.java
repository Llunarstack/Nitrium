package dev.nitrium.client.forge;

import dev.nitrium.client.platform.ClientEvents;
import dev.nitrium.client.platform.ClientRenderStages;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.Identifier;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.gui.overlay.ForgeLayer;
import net.minecraftforge.client.gui.overlay.ForgeLayeredDraw;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;

import java.util.function.Consumer;

/**
 * Binds {@link ClientEvents} to the Forge game event bus (EventBus 7).
 *
 * <p>Forge 61.x removed discrete world-render stage events; render-tick hooks drive the profiler
 * instead. HUD overlays register through {@link AddGuiOverlayLayersEvent}.
 */
public final class ForgeClientEvents implements ClientEvents {
	@Override
	public void clientTickStart(Consumer<net.minecraft.client.Minecraft> callback) {
		TickEvent.ClientTickEvent.Pre.BUS.addListener(
				event -> callback.accept(net.minecraft.client.Minecraft.getInstance()));
	}

	@Override
	public void clientTickEnd(Consumer<net.minecraft.client.Minecraft> callback) {
		TickEvent.ClientTickEvent.Post.BUS.addListener(
				event -> callback.accept(net.minecraft.client.Minecraft.getInstance()));
	}

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
		LevelEvent.Load.BUS.addListener((LevelEvent.Load event) -> {
			if (event.getLevel() instanceof ClientLevel level) {
				callback.accept(level);
			}
		});
		LevelEvent.Unload.BUS.addListener((LevelEvent.Unload event) -> {
			if (event.getLevel() instanceof ClientLevel) {
				callback.accept(null);
			}
		});
		ClientPlayerNetworkEvent.LoggingOut.BUS.addListener(event -> callback.accept(null));
	}

	@Override
	public void clientStopping(Runnable callback) {
		// No Forge client-stopping event; GL resources are freed by the OS on exit.
	}

	@Override
	public void hud(Identifier id, HudLayer layer) {
		AddGuiOverlayLayersEvent.BUS.addListener((AddGuiOverlayLayersEvent event) -> {
			ForgeLayer overlay = (graphics, tickCounter) -> layer.render(graphics, tickCounter);
			event.getLayeredDraw().add(ForgeLayeredDraw.POST_SLEEP_STACK, id, overlay);
		});
	}
}
