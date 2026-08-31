package dev.nitrium.client.gui;

import dev.nitrium.NitriumMod;
import dev.nitrium.memory.NativeResourceCleaner;
import dev.nitrium.client.platform.ClientEvents;
import net.minecraft.client.Minecraft;

import java.lang.ref.Cleaner;

/**
 * SDF text atlas + dirty-tracked HUD layer caching to avoid immediate-mode GUI rebuilds.
 */
public final class GuiRenderEngine {
	private static GuiRenderEngine instance;

	private SdfFontAtlas fontAtlas;
	private HudLayerCache hudCache;
	private final GuiStats stats = new GuiStats();
	private final Cleaner.Cleanable cleanable;
	private int lastHudHash;

	private GuiRenderEngine() {
		this.cleanable = NativeResourceCleaner.register(this, this::destroyGpuResources);
	}

	public static void init() {
		if (instance != null) {
			return;
		}
		instance = new GuiRenderEngine();
		instance.register();
	}

	private void register() {
		fontAtlas = new SdfFontAtlas(2048);

		ClientEvents.get().clientTickEnd(client -> {
			if (client.player == null) {
				return;
			}
			int hash = computeHudStateHash(client);
			if (hash != lastHudHash) {
				lastHudHash = hash;
				if (hudCache != null) {
					hudCache.markDirty();
				}
				stats.recordHudInvalidation();
			}
		});

		NitriumMod.LOGGER.info("Nitrium GUI SDF cache engine active");
	}

	private int computeHudStateHash(Minecraft client) {
		if (client.player == null) {
			return 0;
		}
		int hash = Float.floatToIntBits(client.player.getHealth());
		hash = 31 * hash + client.player.getFoodData().getFoodLevel();
		hash = 31 * hash + client.player.getInventory().getSelectedSlot();
		return hash;
	}

	public void ensureHudCache() {
		Minecraft client = Minecraft.getInstance();
		if (client.getWindow() == null || !dev.nitrium.client.nativegl.GlContext.isReady()) {
			return;
		}
		int width = client.getWindow().getGuiScaledWidth();
		int height = client.getWindow().getGuiScaledHeight();
		if (hudCache == null) {
			hudCache = new HudLayerCache(width, height);
		}
		if (hudCache.isDirty()) {
			stats.recordHudRebuild();
			// TODO: render the static HUD widgets into the hudCache framebuffer.
		}
	}

	public SdfFontAtlas fontAtlas() {
		return fontAtlas;
	}

	public HudLayerCache hudCache() {
		return hudCache;
	}

	public GuiStats stats() {
		return stats;
	}

	public static GuiRenderEngine get() {
		return instance;
	}

	public void onWorldUnload() {
		stats.reset();
		lastHudHash = 0;
		if (hudCache != null) {
			hudCache.markDirty();
		}
	}

	public void shutdown() {
		cleanable.clean();
		instance = null;
	}

	private void destroyGpuResources() {
		if (fontAtlas != null) {
			fontAtlas.close();
			fontAtlas = null;
		}
		if (hudCache != null) {
			hudCache.close();
			hudCache = null;
		}
	}
}
