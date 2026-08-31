package dev.nitrium.client.entity;

import dev.nitrium.Nitrium;

/**
 * Skeletal animation on the GPU: bone matrices blended in a compute pass, written to an SSBO, then
 * consumed by the instanced draw. Not wired up yet — the methods below are stubs waiting on the
 * compute shader.
 */
public final class GpuEntitySkinningCompute {
	private static GpuEntitySkinningCompute instance;
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

		// TODO: compile and link the compute program once the shader exists.
		initialized = true;
		Nitrium.LOGGER.debug("Nitrium GPU entity skinning compute initialized");
	}

	public void dispatch(EntityInstanceBatch batch) {
		if (!initialized || batch.totalInstances() == 0) {
			return;
		}

		// TODO: one glDispatchCompute per entity-type batch.
	}
}
