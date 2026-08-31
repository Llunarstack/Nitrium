package dev.nitrium.client.particles;

import dev.nitrium.client.nativegl.GlContext;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL43;

/**
 * Indirect draw command buffer for batched particle rendering.
 *
 * <p>The GL buffer is created lazily on first draw ({@link GlContext}), never in the constructor,
 * so this is safe to instantiate during client init before the GL context exists.
 */
public final class ParticleIndirectDraw implements AutoCloseable {
	private int commandBufferId;

	public ParticleIndirectDraw() {
	}

	private boolean ensureBuffer() {
		if (commandBufferId != 0) {
			return true;
		}
		if (!GlContext.isReady()) {
			return false;
		}
		commandBufferId = GL15.glGenBuffers();
		GL15.glBindBuffer(GL43.GL_DRAW_INDIRECT_BUFFER, commandBufferId);
		GL15.glBufferData(GL43.GL_DRAW_INDIRECT_BUFFER, 16, GL15.GL_DYNAMIC_DRAW);
		GL15.glBindBuffer(GL43.GL_DRAW_INDIRECT_BUFFER, 0);
		return true;
	}

	public void drawInstanced(int instanceCount) {
		if (instanceCount <= 0 || !ensureBuffer()) {
			return;
		}
		GL15.glBindBuffer(GL43.GL_DRAW_INDIRECT_BUFFER, commandBufferId);
		GL43.glDrawArraysIndirect(GL15.GL_TRIANGLES, 0);
		GL15.glBindBuffer(GL43.GL_DRAW_INDIRECT_BUFFER, 0);
	}

	@Override
	public void close() {
		if (commandBufferId != 0 && GlContext.isReady()) {
			GL15.glDeleteBuffers(commandBufferId);
		}
		commandBufferId = 0;
	}
}
