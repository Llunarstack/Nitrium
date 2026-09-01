package dev.nitrium.client.entity;

import dev.nitrium.Nitrium;
import dev.nitrium.client.nativegl.GlContext;
import dev.nitrium.client.nativegl.GlShaderProgram;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.opengl.GL43;

import java.util.Optional;

/**
 * Placeholder GPU skinning compute pass. Binds the transform SSBO so the instanced draw path can
 * consume it; a full bone-matrix shader can replace {@code entity_skinning.comp} later.
 */
public final class GpuEntitySkinningCompute {
	private static final Identifier SKINNING_SHADER = Identifier.fromNamespaceAndPath("nitrium", "shaders/entity_skinning.comp");

	private static GpuEntitySkinningCompute instance;
	private GlShaderProgram program;
	private boolean initialized;

	private GpuEntitySkinningCompute() {
	}

	public static GpuEntitySkinningCompute get() {
		if (instance == null) {
			instance = new GpuEntitySkinningCompute();
		}
		return instance;
	}

	public void init() {
		if (initialized) {
			return;
		}

		if (GlContext.isReady()) {
			ensureProgram();
		}

		initialized = true;
		Nitrium.LOGGER.debug("Nitrium GPU entity skinning compute initialized");
	}

	public void dispatch(EntityInstanceBatch batch) {
		if (!initialized || batch.totalInstances() == 0 || !ensureProgram()) {
			return;
		}

		EntityTransformBuffer.get().bind();
		program.bind();
		int groups = Math.max(1, (batch.totalInstances() + 63) / 64);
		program.dispatchCompute(groups, 1, 1);
		GL43.glMemoryBarrier(GL43.GL_SHADER_STORAGE_BARRIER_BIT);
		program.unbind();
	}

	private boolean ensureProgram() {
		if (program != null) {
			return true;
		}

		Optional<GlShaderProgram> compiled = GlShaderProgram.compileCompute(
				Minecraft.getInstance().getResourceManager(),
				SKINNING_SHADER
		);
		program = compiled.orElse(null);
		return program != null;
	}
}
