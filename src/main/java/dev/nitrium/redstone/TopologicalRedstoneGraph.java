package dev.nitrium.redstone;

import dev.nitrium.memory.IterativeIntQueue;

import java.util.Arrays;

/**
 * Single-pass BFS topological solver for contiguous redstone wire grids.
 * Replaces recursive vanilla wire recalculation with one sweep per network.
 */
public final class TopologicalRedstoneGraph {
	private static final int MAX_WIRES = 4096;

	private final int[] wireX = new int[MAX_WIRES];
	private final int[] wireY = new int[MAX_WIRES];
	private final int[] wireZ = new int[MAX_WIRES];
	private final int[] power = new int[MAX_WIRES];
	private final boolean[] visited = new boolean[MAX_WIRES];
	private int count;

	public void clear() {
		count = 0;
		Arrays.fill(visited, false);
	}

	public int addWire(int x, int y, int z, int initialPower) {
		if (count >= MAX_WIRES) {
			return -1;
		}
		int index = count++;
		wireX[index] = x;
		wireY[index] = y;
		wireZ[index] = z;
		power[index] = initialPower;
		return index;
	}

	/**
	 * Propagate power from sources using iterative BFS — O(N) single pass.
	 *
	 * @param sourceIndices wire indices that are power sources (strength 15)
	 */
	public void solve(int[] sourceIndices) {
		IterativeIntQueue queue = new IterativeIntQueue(256);
		Arrays.fill(visited, 0, count, false);

		for (int source : sourceIndices) {
			if (source < 0 || source >= count) {
				continue;
			}
			power[source] = 15;
			queue.enqueue(source);
			visited[source] = true;
		}

		while (!queue.isEmpty()) {
			int current = queue.dequeue();
			int currentPower = power[current];
			if (currentPower <= 1) {
				continue;
			}

			int nextPower = currentPower - 1;
			for (int i = 0; i < count; i++) {
				if (visited[i]) {
					continue;
				}
				if (isAdjacent(current, i) && power[i] < nextPower) {
					power[i] = nextPower;
					visited[i] = true;
					queue.enqueue(i);
				}
			}
		}
	}

	public int powerAt(int index) {
		return power[index];
	}

	public int wireCount() {
		return count;
	}

	private boolean isAdjacent(int a, int b) {
		int dx = Math.abs(wireX[a] - wireX[b]);
		int dy = Math.abs(wireY[a] - wireY[b]);
		int dz = Math.abs(wireZ[a] - wireZ[b]);
		return (dx + dy + dz) == 1;
	}
}
