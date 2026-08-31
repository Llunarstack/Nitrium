package dev.nitrium.client.render.oit;

/**
 * Fullscreen pass that resolves the accumulation and revealage buffers back into the main
 * framebuffer using the weighted-blended OIT equation. The resolve shader isn't written yet.
 */
public final class OitCompositePass {
	private boolean ready;

	public void setup() {
		ready = true;
	}

	public void composite(OitFramebuffer buffers) {
		if (!ready || buffers == null) {
			return;
		}
		// TODO: bind the accumulation + revealage textures and draw a fullscreen triangle.
	}

	public void invalidate() {
		ready = false;
	}
}
