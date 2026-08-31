package dev.nitrium.worldgen.taskgraph;

/**
 * Chunk generation pipeline stages for the lock-free task graph.
 */
public enum ChunkGenTaskType {
	NOISE_BIOME,
	DENSITY_SPLINE,
	SURFACE_RULES,
	CAVE_CARVE,
	FEATURES,
	STRUCTURES
}
