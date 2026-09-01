package dev.nitrium.client.render.oit;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.nitrium.client.nativegl.FullscreenQuad;
import dev.nitrium.client.nativegl.GlContext;
import dev.nitrium.client.nativegl.GlShaderProgram;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.util.Optional;

/**
 * Fullscreen pass that resolves weighted-blended OIT accumulation and revealage buffers.
 */
public final class OitCompositePass implements AutoCloseable {
	private static final Identifier VERTEX_SHADER = Identifier.fromNamespaceAndPath("nitrium", "shaders/oit_composite.vert");
	private static final Identifier FRAGMENT_SHADER = Identifier.fromNamespaceAndPath("nitrium", "shaders/oit_composite.frag");

	private GlShaderProgram program;
	private FullscreenQuad quad;
	private boolean ready;

	public void setup() {
		if (!GlContext.isReady()) {
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

		if (quad == null && program != null) {
			quad = new FullscreenQuad();
		}

		ready = program != null && quad != null;
	}

	public void composite(OitFramebuffer buffers) {
		if (!ready || buffers == null) {
			return;
		}

		RenderSystem.assertOnRenderThread();

		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

		program.bind();
		GL13.glActiveTexture(GL13.GL_TEXTURE0);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, buffers.accumulationTexture());
		program.setUniform1i("uAccumulation", 0);

		GL13.glActiveTexture(GL13.GL_TEXTURE1);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, buffers.revealageTexture());
		program.setUniform1i("uRevealage", 1);

		quad.draw();
		program.unbind();

		GL13.glActiveTexture(GL13.GL_TEXTURE0);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
		GL11.glDisable(GL11.GL_BLEND);
	}

	public void invalidate() {
		close();
		ready = false;
	}

	@Override
	public void close() {
		if (program != null) {
			program.close();
			program = null;
		}
		if (quad != null) {
			quad.close();
			quad = null;
		}
	}
}
