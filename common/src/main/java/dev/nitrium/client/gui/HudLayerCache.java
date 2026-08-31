package dev.nitrium.client.gui;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

/**
 * Cached framebuffer for static HUD elements (hotbar frame, hearts, hunger).
 */
public final class HudLayerCache implements AutoCloseable {
	private final int colorTexture;
	private final int framebuffer;
	private final int width;
	private final int height;
	private boolean dirty = true;
	private boolean closed;

	public HudLayerCache(int width, int height) {
		this.width = width;
		this.height = height;

		colorTexture = GL11.glGenTextures();
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, colorTexture);
		GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, width, height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, 0);

		framebuffer = GL30.glGenFramebuffers();
		GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);
		GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, colorTexture, 0);
		GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
	}

	public void markDirty() {
		dirty = true;
	}

	public boolean isDirty() {
		return dirty;
	}

	public void bindForWrite() {
		GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);
		GL11.glViewport(0, 0, width, height);
	}

	public void unbind() {
		GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
		dirty = false;
	}

	public int colorTexture() {
		return colorTexture;
	}

	@Override
	public void close() {
		if (closed) {
			return;
		}
		closed = true;
		GL30.glDeleteFramebuffers(framebuffer);
		GL11.glDeleteTextures(colorTexture);
	}
}
