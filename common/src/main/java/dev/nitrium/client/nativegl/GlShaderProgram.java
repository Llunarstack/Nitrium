package dev.nitrium.client.nativegl;

import dev.nitrium.Nitrium;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL43;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Compiles GLSL shaders from {@code assets/nitrium/shaders/} on the render thread.
 */
public final class GlShaderProgram implements AutoCloseable {
	private final int programId;
	private final Map<String, Integer> uniformLocations = new HashMap<>();

	private GlShaderProgram(int programId) {
		this.programId = programId;
	}

	public static Optional<GlShaderProgram> compileCompute(ResourceManager resources, Identifier sourceId) {
		return compile(resources, sourceId, GL43.GL_COMPUTE_SHADER, true);
	}

	public static Optional<GlShaderProgram> compileGraphics(
			ResourceManager resources,
			Identifier vertexId,
			Identifier fragmentId
	) {
		if (!GlContext.isReady()) {
			return Optional.empty();
		}

		Optional<String> vertex = readSource(resources, vertexId);
		Optional<String> fragment = readSource(resources, fragmentId);
		if (vertex.isEmpty() || fragment.isEmpty()) {
			return Optional.empty();
		}

		int vertexShader = compileShader(GL20.GL_VERTEX_SHADER, vertex.get());
		int fragmentShader = compileShader(GL20.GL_FRAGMENT_SHADER, fragment.get());
		if (vertexShader == 0 || fragmentShader == 0) {
			if (vertexShader != 0) {
				GL20.glDeleteShader(vertexShader);
			}
			if (fragmentShader != 0) {
				GL20.glDeleteShader(fragmentShader);
			}
			return Optional.empty();
		}

		int program = GL20.glCreateProgram();
		GL20.glAttachShader(program, vertexShader);
		GL20.glAttachShader(program, fragmentShader);
		GL20.glLinkProgram(program);

		GL20.glDeleteShader(vertexShader);
		GL20.glDeleteShader(fragmentShader);

		if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL20.GL_FALSE) {
			Nitrium.LOGGER.warn("Shader link failed ({} + {}): {}", vertexId, fragmentId, GL20.glGetProgramInfoLog(program));
			GL20.glDeleteProgram(program);
			return Optional.empty();
		}

		return Optional.of(new GlShaderProgram(program));
	}

	private static Optional<GlShaderProgram> compile(
			ResourceManager resources,
			Identifier sourceId,
			int shaderType,
			boolean isCompute
	) {
		if (!GlContext.isReady()) {
			return Optional.empty();
		}

		Optional<String> source = readSource(resources, sourceId);
		if (source.isEmpty()) {
			return Optional.empty();
		}

		int shader = compileShader(shaderType, source.get());
		if (shader == 0) {
			return Optional.empty();
		}

		int program = GL20.glCreateProgram();
		GL20.glAttachShader(program, shader);
		GL20.glLinkProgram(program);
		GL20.glDeleteShader(shader);

		if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL20.GL_FALSE) {
			Nitrium.LOGGER.warn("Shader link failed ({}): {}", sourceId, GL20.glGetProgramInfoLog(program));
			GL20.glDeleteProgram(program);
			return Optional.empty();
		}

		return Optional.of(new GlShaderProgram(program));
	}

	private static int compileShader(int type, String source) {
		int shader = GL20.glCreateShader(type);
		GL20.glShaderSource(shader, source);
		GL20.glCompileShader(shader);
		if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL20.GL_FALSE) {
			Nitrium.LOGGER.warn("Shader compile failed: {}", GL20.glGetShaderInfoLog(shader));
			GL20.glDeleteShader(shader);
			return 0;
		}
		return shader;
	}

	private static Optional<String> readSource(ResourceManager resources, Identifier id) {
		try {
			Optional<Resource> resource = resources.getResource(id);
			if (resource.isEmpty()) {
				Nitrium.LOGGER.warn("Missing shader resource: {}", id);
				return Optional.empty();
			}
			try (InputStream stream = resource.get().open()) {
				return Optional.of(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
			}
		} catch (IOException exception) {
			Nitrium.LOGGER.warn("Failed to read shader {}", id, exception);
			return Optional.empty();
		}
	}

	public void bind() {
		GL20.glUseProgram(programId);
	}

	public void unbind() {
		GL20.glUseProgram(0);
	}

	public int programId() {
		return programId;
	}

	public int uniform(String name) {
		return uniformLocations.computeIfAbsent(name, key -> GL20.glGetUniformLocation(programId, key));
	}

	public void setUniform1i(String name, int value) {
		GL20.glUniform1i(uniform(name), value);
	}

	public void setUniform1f(String name, float value) {
		GL20.glUniform1f(uniform(name), value);
	}

	public void setUniform2f(String name, float x, float y) {
		GL20.glUniform2f(uniform(name), x, y);
	}

	public void setUniform4f(String name, float x, float y, float z, float w) {
		GL20.glUniform4f(uniform(name), x, y, z, w);
	}

	public void setUniformMatrix4f(String name, org.joml.Matrix4f matrix) {
		float[] values = new float[16];
		matrix.get(values);
		GL20.glUniformMatrix4fv(uniform(name), false, values);
	}

	public void dispatchCompute(int groupsX, int groupsY, int groupsZ) {
		GL43.glDispatchCompute(groupsX, groupsY, groupsZ);
	}

	@Override
	public void close() {
		if (GlContext.isReady()) {
			GL20.glDeleteProgram(programId);
		}
	}
}
