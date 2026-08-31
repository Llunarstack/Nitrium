package dev.nitrium.client.audio;

import java.util.Arrays;

/**
 * Coarse 4×4×4 voxel grid for fast wall occlusion lookups (no per-ray mesh collision).
 */
public final class AudioVoxelGrid {
	private final int cellSize;
	private final byte[] cells = new byte[4096];
	private int originX;
	private int originY;
	private int originZ;

	public AudioVoxelGrid(int cellSizeBlocks) {
		this.cellSize = cellSizeBlocks;
	}

	public void setOrigin(int blockX, int blockY, int blockZ) {
		originX = blockX;
		originY = blockY;
		originZ = blockZ;
		Arrays.fill(cells, (byte) 0);
	}

	public void setSolid(int localX, int localY, int localZ) {
		int index = index(localX, localY, localZ);
		if (index >= 0 && index < cells.length) {
			cells[index] = 1;
		}
	}

	public boolean isOccluded(int fromX, int fromY, int fromZ, int toX, int toY, int toZ) {
		int steps = Math.max(Math.abs(toX - fromX), Math.max(Math.abs(toY - fromY), Math.abs(toZ - fromZ)));
		if (steps == 0) {
			return false;
		}
		for (int i = 1; i < steps; i++) {
			int x = fromX + (toX - fromX) * i / steps;
			int y = fromY + (toY - fromY) * i / steps;
			int z = fromZ + (toZ - fromZ) * i / steps;
			int lx = (x - originX) / cellSize;
			int ly = (y - originY) / cellSize;
			int lz = (z - originZ) / cellSize;
			int index = index(lx, ly, lz);
			if (index >= 0 && index < cells.length && cells[index] != 0) {
				return true;
			}
		}
		return false;
	}

	private int index(int x, int y, int z) {
		if (x < 0 || y < 0 || z < 0 || x >= 16 || y >= 16 || z >= 16) {
			return -1;
		}
		return x + y * 16 + z * 256;
	}
}
