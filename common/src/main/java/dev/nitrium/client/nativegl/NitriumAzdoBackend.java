package dev.nitrium.client.nativegl;

import dev.nitrium.Nitrium;
import dev.nitrium.config.NitriumConfigManager;
import dev.nitrium.memory.NativeResourceCleaner;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.platform.Window;

/**
 * AZDO OpenGL backend: persistent-mapped geometry buffer plus a multi-draw-indirect batch. Kept on
 * GL (rather than a Vulkan/RT path) so Iris shader compatibility holds. The buffers are allocated
 * but nothing submits geometry through them yet.
 */
public final class NitriumAzdoBackend implements AutoCloseable {
	private static NitriumAzdoBackend instance;

	private PersistentMappedBuffer geometryBuffer;
	private MultiDrawIndirectBatch indirectBatch;

	// Dynamic resolution state fed by the QualityGovernor each frame.
	private static final float SCALE_EPSILON = 0.005f;
	private float appliedScale = 1.0f;
	private int scaledWidth;
	private int scaledHeight;

	private NitriumAzdoBackend() {
	}

	public static void init() {
		if (instance != null) {
			return;
		}

		if (!NitriumConfigManager.get().enableAzdoBackend) {
			Nitrium.LOGGER.info("Nitrium AZDO backend disabled via config");
			return;
		}

		instance = new NitriumAzdoBackend();
		instance.setup();
	}

	private void setup() {
		// No GL here: this runs during client init before the GL context is ready. The persistent
		// buffer and MDI batch are allocated lazily on the render thread via ensureGlResources().
		Nitrium.LOGGER.info("Nitrium AZDO backend configured (GL resources allocated on first render use)");
	}

	/**
	 * Allocate the GL-backed geometry buffer and MDI batch the first time they are needed on the
	 * render thread. Returns {@code false} until the GL context is ready.
	 */
	private synchronized boolean ensureGlResources() {
		if (geometryBuffer != null) {
			return true;
		}
		if (!GlContext.isReady()) {
			return false;
		}

		if (!PersistentMappedBuffer.isSupported()) {
			Nitrium.LOGGER.warn("GL_ARB_buffer_storage unavailable — AZDO falling back to a dynamic buffer");
		}

		long bufferBytes = (long) NitriumConfigManager.get().geometryBufferBudgetMb * 1024L * 1024L;
		geometryBuffer = new PersistentMappedBuffer(bufferBytes);
		indirectBatch = new MultiDrawIndirectBatch(8192);
		NativeResourceCleaner.register(this, () -> {
			if (geometryBuffer != null) {
				geometryBuffer.close();
			}
			if (indirectBatch != null) {
				indirectBatch.close();
			}
		});
		Nitrium.LOGGER.info("Nitrium AZDO backend ready (persistent={})", geometryBuffer.isPersistent());
		return true;
	}

	public static NitriumAzdoBackend get() {
		return instance;
	}

	/**
	 * Accept the governor's recommended linear render scale and recompute the target
	 * framebuffer dimensions for the world pass. This is the resolution input the MDI world
	 * pass allocates its color/depth attachments at; on vanilla (no scaled render target) it
	 * is tracked and surfaced to the overlay but does not resize the main framebuffer.
	 *
	 * @return {@code true} if the applied scale changed beyond the epsilon threshold
	 */
	public boolean onRenderScale(float scale) {
		float clamped = Math.clamp(scale, 0.1f, 1.0f);
		Window window = windowOrNull();
		if (window != null) {
			scaledWidth = Math.max(1, Math.round(window.getWidth() * clamped));
			scaledHeight = Math.max(1, Math.round(window.getHeight() * clamped));
		}

		if (Math.abs(clamped - appliedScale) < SCALE_EPSILON) {
			return false;
		}

		appliedScale = clamped;
		Nitrium.LOGGER.debug("Nitrium AZDO render scale -> {}% ({}x{})",
				Math.round(clamped * 100.0f), scaledWidth, scaledHeight);
		return true;
	}

	public float appliedScale() {
		return appliedScale;
	}

	public int scaledFramebufferWidth() {
		return scaledWidth;
	}

	public int scaledFramebufferHeight() {
		return scaledHeight;
	}

	private static Window windowOrNull() {
		Minecraft client = Minecraft.getInstance();
		return client != null ? client.getWindow() : null;
	}

	public PersistentMappedBuffer geometryBuffer() {
		ensureGlResources();
		return geometryBuffer;
	}

	public MultiDrawIndirectBatch indirectBatch() {
		ensureGlResources();
		return indirectBatch;
	}

	public void close() {
		if (geometryBuffer != null) {
			geometryBuffer.close();
			geometryBuffer = null;
		}
		if (indirectBatch != null) {
			indirectBatch.close();
			indirectBatch = null;
		}
		instance = null;
	}
}
