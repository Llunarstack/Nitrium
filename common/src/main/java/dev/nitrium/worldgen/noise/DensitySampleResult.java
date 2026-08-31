package dev.nitrium.worldgen.noise;

/**
 * Result of coarse-grid density sampling for one chunk section.
 */
public record DensitySampleResult(float[] density, int refinedVoxelCount, int coarseSampleCount) {
}
