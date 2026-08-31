package dev.nitrium.client.render.oit;

import dev.nitrium.NitriumMod;
import dev.nitrium.memory.NativeResourceCleaner;
import dev.nitrium.client.platform.ClientEvents;
import net.minecraft.client.Minecraft;

import java.lang.ref.Cleaner;

/**
 * Weighted blended OIT pipeline — eliminates CPU translucent depth sorting.
 */
public final class WeightedBlendedOitPipeline {
	private static WeightedBlendedOitPipeline instance;

	private OitFramebuffer buffers;
	private final OitCompositePass compositePass = new OitCompositePass();
	private final OitStats stats = new OitStats();
	private final Cleaner.Cleanable cleanable;
	private int lastWidth;
	private int lastHeight;

	private WeightedBlendedOitPipeline() {
		this.cleanable = NativeResourceCleaner.register(this, this::destroyGpuResources);
	}

	public static void init() {
		if (instance != null) {
			return;
		}
		instance = new WeightedBlendedOitPipeline();
		instance.register();
	}

	private void register() {
		ClientEvents events = ClientEvents.get();
		events.worldRenderAfterEntities(this::beginTranslucentPass);
		events.worldRenderEnd(this::endTranslucentPass);

		NitriumMod.LOGGER.info("Nitrium OIT translucency pipeline active (weighted blended)");
	}

	private void beginTranslucentPass() {
		stats.recordFrame();
		ensureBuffers();
		if (buffers != null) {
			buffers.clear();
			buffers.bind();
		}
	}

	private void endTranslucentPass() {
		if (buffers != null) {
			buffers.unbind();
			compositePass.composite(buffers);
			stats.recordComposite();
		}
	}

	private void ensureBuffers() {
		Minecraft client = Minecraft.getInstance();
		if (client.getWindow() == null) {
			return;
		}
		int width = client.getWindow().getWidth();
		int height = client.getWindow().getHeight();
		if (buffers == null || width != lastWidth || height != lastHeight) {
			destroyGpuResources();
			buffers = new OitFramebuffer(width, height);
			compositePass.setup();
			lastWidth = width;
			lastHeight = height;
		}
	}

	public OitStats stats() {
		return stats;
	}

	public static WeightedBlendedOitPipeline get() {
		return instance;
	}

	public void onWorldUnload() {
		stats.reset();
	}

	public void shutdown() {
		cleanable.clean();
		instance = null;
	}

	private void destroyGpuResources() {
		if (buffers != null) {
			buffers.close();
			buffers = null;
		}
		compositePass.invalidate();
	}
}
