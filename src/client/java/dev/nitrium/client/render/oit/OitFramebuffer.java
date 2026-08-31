package dev.nitrium.client.render.oit;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

/**
 * Offscreen accumulation (RGBA16F) and revealage (R8) buffers for weighted blended OIT.
 */
public final class OitFramebuffer implements AutoCloseable {
	private final int accumulationTexture;
	private final int revealageTexture;
	private final int framebuffer;
	private final int width;
	private final int height;
	private boolean closed;

	public OitFramebuffer(int width, int height) {
		this.width = width;
		this.height = height;

		accumulationTexture = GL11.glGenTextures();
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, accumulationTexture);
		GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_RGBA16F, width, height, 0, GL11.GL_RGBA, GL11.GL_FLOAT, 0);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);

		revealageTexture = GL11.glGenTextures();
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, revealageTexture);
		GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_R8, width, height, 0, GL11.GL_RED, GL11.GL_UNSIGNED_BYTE, 0);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);

		framebuffer = GL30.glGenFramebuffers();
		GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);
		GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, accumulationTexture, 0);
		GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT1, GL11.GL_TEXTURE_2D, revealageTexture, 0);

		int[] drawBuffers = {GL30.GL_COLOR_ATTACHMENT0, GL30.GL_COLOR_ATTACHMENT1};
		GL30.glDrawBuffers(drawBuffers);

		GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
	}

	public int accumulationTexture() {
		return accumulationTexture;
	}

	public int revealageTexture() {
		return revealageTexture;
	}

	public int framebuffer() {
		return framebuffer;
	}

	public void bind() {
		GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);
		GL11.glViewport(0, 0, width, height);
	}

	public void unbind() {
		GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
	}

	public void clear() {
		bind();
		GL11.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
		GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
		unbind();
	}

	@Override
	public void close() {
		if (closed) {
			return;
		}
		closed = true;
		GL30.glDeleteFramebuffers(framebuffer);
		GL11.glDeleteTextures(accumulationTexture);
		GL11.glDeleteTextures(revealageTexture);
	}
}
