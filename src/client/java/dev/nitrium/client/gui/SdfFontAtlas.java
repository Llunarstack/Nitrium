package dev.nitrium.client.gui;

import dev.nitrium.client.nativegl.GlContext;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

/**
 * Signed Distance Field glyph atlas for scalable text without per-frame vertex rebuild.
 *
 * <p>The GL texture is created lazily on first use ({@link GlContext}), never in the constructor,
 * so this may be instantiated during client init before the GL context exists.
 */
public final class SdfFontAtlas implements AutoCloseable {
	private int textureId;
	private final int atlasSize;
	private boolean closed;

	public SdfFontAtlas(int atlasSize) {
		this.atlasSize = atlasSize;
	}

	private void ensureTexture() {
		if (textureId != 0 || closed || !GlContext.isReady()) {
			return;
		}
		textureId = GL11.glGenTextures();
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
		GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_R8, atlasSize, atlasSize, 0, GL11.GL_RED, GL11.GL_UNSIGNED_BYTE, 0);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
	}

	public int textureId() {
		ensureTexture();
		return textureId;
	}

	public int atlasSize() {
		return atlasSize;
	}

	@Override
	public void close() {
		if (closed) {
			return;
		}
		closed = true;
		if (textureId != 0 && GlContext.isReady()) {
			GL11.glDeleteTextures(textureId);
		}
		textureId = 0;
	}
}
