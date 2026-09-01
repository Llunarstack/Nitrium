package dev.nitrium.client.culling.entity;

import dev.nitrium.Nitrium;
import dev.nitrium.client.culling.terrain.HiZOcclusionCuller;
import dev.nitrium.client.nativegl.GlContext;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL33;

/**
 * GPU occlusion queries for entity proxy boxes. Uses the Hi-Z pyramid when available; maintains a
 * low-res depth FBO for future proxy rendering.
 */
public final class GpuEntityOcclusionQuery {
	private static GpuEntityOcclusionQuery instance;

	private boolean initialized;
	private int framebuffer;
	private int depthTexture;
	private int bufferSize;

	private GpuEntityOcclusionQuery() {
	}

	public static GpuEntityOcclusionQuery get() {
		if (instance == null) {
			instance = new GpuEntityOcclusionQuery();
		}
		return instance;
	}

	public void init(int bufferSize) {
		if (initialized) {
			return;
		}

		this.bufferSize = bufferSize;
		if (GlContext.isReady()) {
			allocateFramebuffer();
		}

		initialized = true;
		Nitrium.LOGGER.debug("Nitrium entity occlusion proxy buffer: {}x{}", bufferSize, bufferSize);
	}

	public boolean isOccluded(int queryId) {
		return false;
	}

	public boolean isOccludedByHiZ(net.minecraft.world.phys.AABB bounds) {
		return HiZOcclusionCuller.get().isOccluded(bounds);
	}

	public void beginFrame() {
		if (!initialized || framebuffer == 0) {
			if (initialized && GlContext.isReady() && framebuffer == 0) {
				allocateFramebuffer();
			}
			return;
		}

		GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);
		GL11.glViewport(0, 0, bufferSize, bufferSize);
		GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
	}

	public void endFrame() {
		if (framebuffer != 0) {
			GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
		}
	}

	private void allocateFramebuffer() {
		depthTexture = GL11.glGenTextures();
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, depthTexture);
		GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL14.GL_DEPTH_COMPONENT24, bufferSize, bufferSize, 0,
				GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, 0);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);

		framebuffer = GL30.glGenFramebuffers();
		GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);
		GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL11.GL_TEXTURE_2D, depthTexture, 0);
		GL11.glDrawBuffer(GL11.GL_NONE);
		GL11.glReadBuffer(GL11.GL_NONE);

		int status = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
		if (status != GL30.GL_FRAMEBUFFER_COMPLETE) {
			Nitrium.LOGGER.warn("Entity occlusion FBO incomplete: {}", status);
			GL30.glDeleteFramebuffers(framebuffer);
			GL11.glDeleteTextures(depthTexture);
			framebuffer = 0;
			depthTexture = 0;
		}

		GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
	}
}
