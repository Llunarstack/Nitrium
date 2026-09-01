package dev.nitrium.client.fabric;

import dev.nitrium.client.platform.ClientEvents;
import dev.nitrium.client.platform.ClientRenderStages;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.Identifier;

import java.util.function.Consumer;

/**
 * Binds {@link ClientEvents} to the Fabric API client tick, world-render, HUD and lifecycle events.
 */
public final class FabricClientEvents implements ClientEvents {
	@Override
	public void clientTickStart(Consumer<Minecraft> callback) {
		ClientTickEvents.START_CLIENT_TICK.register(callback::accept);
	}

	@Override
	public void clientTickEnd(Consumer<Minecraft> callback) {
		ClientTickEvents.END_CLIENT_TICK.register(callback::accept);
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
		ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((client, world) -> callback.accept(world));
	}

	@Override
	public void clientStopping(Runnable callback) {
		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> callback.run());
	}

	@Override
	public void hud(Identifier id, HudLayer layer) {
		HudElementRegistry.attachElementBefore(
				VanillaHudElements.DEMO_TIMER,
				id,
				layer::render
		);
	}
}
