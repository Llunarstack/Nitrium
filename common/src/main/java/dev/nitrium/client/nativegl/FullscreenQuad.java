package dev.nitrium.client.nativegl;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

/**
 * Single VAO/VBO for a fullscreen triangle (covers the entire viewport).
 */
public final class FullscreenQuad implements AutoCloseable {
	private final int vaoId;
	private final int vboId;

	public FullscreenQuad() {
		float[] vertices = {
				-1.0f, -1.0f,
				3.0f, -1.0f,
				-1.0f, 3.0f
		};

		vaoId = GL30.glGenVertexArrays();
		vboId = GL15.glGenBuffers();

		GL30.glBindVertexArray(vaoId);
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertices, GL15.GL_STATIC_DRAW);
		GL20.glEnableVertexAttribArray(0);
		GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 8, 0);
		GL30.glBindVertexArray(0);
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
	}

	public void draw() {
		GL30.glBindVertexArray(vaoId);
		GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 3);
		GL30.glBindVertexArray(0);
	}

	@Override
	public void close() {
		if (GlContext.isReady()) {
			GL30.glDeleteVertexArrays(vaoId);
			GL15.glDeleteBuffers(vboId);
		}
	}
}
