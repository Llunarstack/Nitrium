package dev.nitrium.memory;

import java.util.function.IntConsumer;

/**
 * Iterative 3D flood-fill without recursion — prevents stack overflow on deep cave graphs.
 */
public final class ChunkGraphTraversal {
	private ChunkGraphTraversal() {
	}

	/**
	 * @param start starting node id
	 * @param neighborCount returns number of neighbors for a node
	 * @param neighborAt returns neighbor id at index for a node
	 * @param onVisit called for each newly discovered node
	 */
	public static void breadthFirst(
			int start,
			IntNeighborCount neighborCount,
			NeighborLookup neighborAt,
			IntConsumer onVisit
	) {
		IterativeIntQueue queue = new IterativeIntQueue(512);
		java.util.BitSet visited = new java.util.BitSet();

		queue.enqueue(start);
		visited.set(start);

		while (!queue.isEmpty()) {
			int current = queue.dequeue();
			onVisit.accept(current);

			int neighbors = neighborCount.count(current);
			for (int i = 0; i < neighbors; i++) {
				int next = neighborAt.get(current, i);
				if (next < 0 || visited.get(next)) {
					continue;
				}
				visited.set(next);
				queue.enqueue(next);
			}
		}
	}

	@FunctionalInterface
	public interface IntNeighborCount {
		int count(int node);
	}

	@FunctionalInterface
	public interface NeighborLookup {
		int get(int node, int index);
	}
}
