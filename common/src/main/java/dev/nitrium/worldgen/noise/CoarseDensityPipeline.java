package dev.nitrium.worldgen.noise;

import dev.nitrium.config.NitriumConfigManager;
import dev.nitrium.nativecore.SimdNoise3D;
import dev.nitrium.worldgen.pool.ZeroAllocWorldgenContext;

/**
 * Coarse-grid density pipeline: sample at 4×8×4, trilinear fill, refine high-gradient voxels.
 */
public final class CoarseDensityPipeline {
	private final CoarseGridConfig grid;
	private final SimdNoiseEngine noiseEngine;

	public CoarseDensityPipeline(CoarseGridConfig grid) {
		this.grid = grid;
		this.noiseEngine = new SimdNoiseEngine();
	}

	public static CoarseDensityPipeline create() {
		return new CoarseDensityPipeline(CoarseGridConfig.fromConfig());
	}

	/**
	 * Fills a per-block density buffer for a 16×height×16 chunk section using coarse sampling.
	 *
	 * @param chunkOriginX world block X of chunk origin
	 * @param chunkOriginY world block Y of section base
	 * @param chunkOriginZ world block Z of chunk origin
	 * @param sizeX typically 16
	 * @param sizeY section height (up to 384 in overworld)
	 * @param sizeZ typically 16
	 * @param seed worldgen seed fragment
	 */
	public DensitySampleResult sampleChunkSection(
			int chunkOriginX,
			int chunkOriginY,
			int chunkOriginZ,
			int sizeX,
			int sizeY,
			int sizeZ,
			int seed
	) {
		ZeroAllocWorldgenContext ctx = ZeroAllocWorldgenContext.current();
		int coarseX = grid.coarseSizeX(sizeX);
		int coarseY = grid.coarseSizeY(sizeY);
		int coarseZ = grid.coarseSizeZ(sizeZ);

		float[] coarse = ctx.borrowFloats(grid.coarseSampleCount(sizeX, sizeY, sizeZ));
		float[] density = ctx.borrowFloats(sizeX * sizeY * sizeZ);
		byte[] refineMask = ctx.borrowBytes(sizeX * sizeY * sizeZ);

		noiseEngine.fillCoarse(
				coarse,
				coarseX,
				coarseY,
				coarseZ,
				chunkOriginX,
				chunkOriginY,
				chunkOriginZ,
				grid.stepX,
				grid.stepY,
				grid.stepZ,
				seed
		);

		SimdNoise3D.trilinearFill(
				density,
				sizeX,
				sizeY,
				sizeZ,
				coarse,
				coarseX,
				coarseY,
				coarseZ,
				grid.stepX,
				grid.stepY,
				grid.stepZ
		);

		int refined = 0;
		if (NitriumConfigManager.get().enableSimdNoise) {
			SimdNoise3D.markHighGradient(refineMask, sizeX, sizeY, sizeZ, density, grid.highGradientThreshold);
			refined = refineHighGradientVoxels(
					density,
					refineMask,
					sizeX,
					sizeY,
					sizeZ,
					chunkOriginX,
					chunkOriginY,
					chunkOriginZ,
					seed
			);
		}

		return new DensitySampleResult(density, refined, coarse.length);
	}

	private int refineHighGradientVoxels(
			float[] density,
			byte[] mask,
			int sizeX,
			int sizeY,
			int sizeZ,
			int originX,
			int originY,
			int originZ,
			int seed
	) {
		int plane = sizeX * sizeY;
		int refined = 0;
		for (int z = 0; z < sizeZ; z++) {
			for (int y = 0; y < sizeY; y++) {
				for (int x = 0; x < sizeX; x++) {
					int i = x + y * sizeX + z * plane;
					if (mask[i] == 0) {
						continue;
					}
					density[i] = noiseEngine.sampleFullResolution(
							originX + x,
							originY + y,
							originZ + z,
							seed
					);
					refined++;
				}
			}
		}
		return refined;
	}
}
