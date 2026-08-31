package dev.nitrium.client.platform;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.Identifier;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Loader-agnostic client-side event hooks: client tick, the world-render pass boundaries, HUD
 * rendering, and lifecycle. The active loader installs an implementation; client code registers
 * through {@link #get()}. None of Nitrium's render callbacks need the loader's render context, so
 * the world-render hooks are plain {@link Runnable}s, which map cleanly onto every loader.
 */
public interface ClientEvents {
	@FunctionalInterface
	interface HudLayer {
		void render(GuiGraphics graphics, DeltaTracker deltaTracker);
	}

	void clientTickStart(Consumer<Minecraft> callback);

	void clientTickEnd(Consumer<Minecraft> callback);

	void worldRenderStart(Runnable callback);

	void worldRenderBeforeEntities(Runnable callback);

	void worldRenderAfterEntities(Runnable callback);

	void worldRenderBeforeDebug(Runnable callback);

	void worldRenderEnd(Runnable callback);

	/** Fired after the client switches worlds; the argument is the new current world (may be null). */
	void clientWorldChanged(Consumer<ClientLevel> callback);

	void clientStopping(Runnable callback);

	void hud(Identifier id, HudLayer layer);

	static ClientEvents get() {
		return Holder.instance;
	}

	static void install(ClientEvents events) {
		Holder.instance = events;
	}

	final class Holder {
		private static ClientEvents instance;

		private Holder() {
		}
	}
}
