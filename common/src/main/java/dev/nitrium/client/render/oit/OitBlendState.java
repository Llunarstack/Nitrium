package dev.nitrium.client.render.oit;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

/**
 * OpenGL blend state for weighted blended order-independent transparency.
 */
public final class OitBlendState {
	private OitBlendState() {
	}

	public static void enable() {
		GL11.glEnable(GL11.GL_BLEND);
		GL14.glBlendFuncSeparate(GL11.GL_ONE, GL11.GL_ONE, GL11.GL_ZERO, GL11.GL_ONE_MINUS_SRC_ALPHA);
	}

	public static void disable() {
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
	}
}
