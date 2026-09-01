package dev.nitrium.client.entity;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.nitrium.client.nativegl.GlContext;
import dev.nitrium.client.nativegl.GlShaderProgram;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL43;

import java.util.Optional;

/**
 * Issues instanced proxy draws for entities batched in {@link EntityInstanceBatch}.
 */
public final class EntityInstancedRenderer implements AutoCloseable {
	private static final Identifier VERTEX_SHADER = Identifier.fromNamespaceAndPath("nitrium", "shaders/entity_instanced.vert");
	private static final Identifier FRAGMENT_SHADER = Identifier.fromNamespaceAndPath("nitrium", "shaders/entity_instanced.frag");

	private GlShaderProgram program;
	private int vao;
	private int vbo;
	private boolean ready;

	public void ensureReady() {
		if (ready || !GlContext.isReady()) {
			return;
		}

		if (program == null) {
			Optional<GlShaderProgram> compiled = GlShaderProgram.compileGraphics(
					Minecraft.getInstance().getResourceManager(),
					VERTEX_SHADER,
					FRAGMENT_SHADER
			);
			program = compiled.orElse(null);
		}

		if (program != null && vao == 0) {
			float[] corners = {
					-0.25f, 0.0f, -0.25f,
					0.25f, 0.0f, -0.25f,
					0.25f, 2.0f, -0.25f,
					-0.25f, 2.0f, -0.25f,
					-0.25f, 0.0f, 0.25f,
					0.25f, 0.0f, 0.25f,
					0.25f, 2.0f, 0.25f,
					-0.25f, 2.0f, 0.25f
			};
			vao = GL30.glGenVertexArrays();
			vbo = GL15.glGenBuffers();
			GL30.glBindVertexArray(vao);
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
			GL15.glBufferData(GL15.GL_ARRAY_BUFFER, corners, GL15.GL_STATIC_DRAW);
			GL20.glEnableVertexAttribArray(0);
			GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 0, 0);
			GL30.glBindVertexArray(0);
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
		}

		ready = program != null && vao != 0;
	}

	public void draw(EntityInstanceBatch batch) {
		int count = batch.totalInstances();
		if (count <= 0) {
			return;
		}

		ensureReady();
		if (!ready) {
			return;
		}

		RenderSystem.assertOnRenderThread();
		Minecraft client = Minecraft.getInstance();
		float partialTick = client.getDeltaTracker().getGameTimeDeltaPartialTick(false);
		var camera = client.gameRenderer.getMainCamera();

		Matrix4f projection = new Matrix4f(client.gameRenderer.getProjectionMatrix(partialTick));
		Matrix4f view = new Matrix4f();
		Quaternionf rotation = camera.rotation();
		view.rotate(rotation.conjugate(new Quaternionf()));
		view.translate(
				-(float) camera.position().x,
				-(float) camera.position().y,
				-(float) camera.position().z
		);

		EntityTransformBuffer.get().bind();
		program.bind();
		program.setUniformMatrix4f("uProjection", projection);
		program.setUniformMatrix4f("uView", view);

		GL30.glBindVertexArray(vao);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glDepthMask(false);
		GL31.glDrawArraysInstanced(GL11.GL_TRIANGLE_FAN, 0, 4, Math.min(count, 4096));
		GL31.glDrawArraysInstanced(GL11.GL_TRIANGLE_FAN, 4, 4, Math.min(count, 4096));
		GL11.glDepthMask(true);
		GL11.glDisable(GL11.GL_BLEND);
		GL30.glBindVertexArray(0);
		program.unbind();
		GL43.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 1, 0);
	}

	@Override
	public void close() {
		if (program != null) {
			program.close();
			program = null;
		}
		if (GlContext.isReady()) {
			if (vao != 0) {
				GL30.glDeleteVertexArrays(vao);
				vao = 0;
			}
			if (vbo != 0) {
				GL15.glDeleteBuffers(vbo);
				vbo = 0;
			}
		}
		ready = false;
	}
}
