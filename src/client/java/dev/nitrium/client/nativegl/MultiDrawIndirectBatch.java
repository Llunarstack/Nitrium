package dev.nitrium.client.nativegl;

import dev.nitrium.NitriumMod;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL43;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

/**
 * An AZDO multi-draw-indirect command buffer: fill it from cull results, then issue a single
 * {@code glMultiDrawElementsIndirect}. The buffer plumbing works; nothing feeds it commands yet.
 */
public final class MultiDrawIndirectBatch implements AutoCloseable {
	private final int commandBufferId;
	private final ByteBuffer commandMemory;
	private int drawCount;

	public MultiDrawIndirectBatch(int maxDraws) {
		commandBufferId = GL15.glGenBuffers();
		int bytes = maxDraws * 5 * Integer.BYTES; // DrawElementsIndirectCommand
		commandMemory = MemoryUtil.memAlloc(bytes);

		GL15.glBindBuffer(GL43.GL_DRAW_INDIRECT_BUFFER, commandBufferId);
		GL15.glBufferData(GL43.GL_DRAW_INDIRECT_BUFFER, commandMemory.capacity(), GL15.GL_DYNAMIC_DRAW);
		GL15.glBindBuffer(GL43.GL_DRAW_INDIRECT_BUFFER, 0);

		NitriumMod.LOGGER.debug("MDI command buffer: {} max draws", maxDraws);
	}

	public void clear() {
		drawCount = 0;
		commandMemory.clear();
	}

	public void addCommand(int indexCount, int instanceCount, int firstIndex, int baseVertex, int baseInstance) {
		commandMemory.putInt(indexCount);
		commandMemory.putInt(instanceCount);
		commandMemory.putInt(firstIndex);
		commandMemory.putInt(baseVertex);
		commandMemory.putInt(baseInstance);
		drawCount++;
	}

	public void upload() {
		commandMemory.flip();
		GL15.glBindBuffer(GL43.GL_DRAW_INDIRECT_BUFFER, commandBufferId);
		GL15.glBufferSubData(GL43.GL_DRAW_INDIRECT_BUFFER, 0, commandMemory);
		GL15.glBindBuffer(GL43.GL_DRAW_INDIRECT_BUFFER, 0);
	}

	/**
	 * Issue indirect draw. Caller must bind VAO and element buffer first.
	 */
	public void draw() {
		if (drawCount == 0) {
			return;
		}

		GL15.glBindBuffer(GL43.GL_DRAW_INDIRECT_BUFFER, commandBufferId);
		GL43.glMultiDrawElementsIndirect(GL15.GL_TRIANGLES, GL15.GL_UNSIGNED_INT, 0, drawCount, 0);
		GL15.glBindBuffer(GL43.GL_DRAW_INDIRECT_BUFFER, 0);
	}

	public int drawCount() {
		return drawCount;
	}

	@Override
	public void close() {
		GL15.glDeleteBuffers(commandBufferId);
		MemoryUtil.memFree(commandMemory);
	}
}
