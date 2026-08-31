package dev.nitrium.lighting;

import dev.nitrium.memory.IterativeIntQueue;

import java.util.ArrayList;
import java.util.List;

/**
 * Directed acyclic graph of light propagation tasks per chunk section.
 * Uses iterative BFS — no recursive flood-fill stack overflow.
 */
public final class LightPropagationDag {
	private final List<LightPropagationNode> nodes = new ArrayList<>();

	public LightPropagationNode addNode(int sectionIndex, Runnable propagation) {
		LightPropagationNode node = new LightPropagationNode(sectionIndex, propagation);
		nodes.add(node);
		return node;
	}

	public void addDependency(LightPropagationNode from, LightPropagationNode to) {
		to.addDependency(from);
	}

	public void executeReady() {
		IterativeIntQueue queue = new IterativeIntQueue(nodes.size());
		for (int i = 0; i < nodes.size(); i++) {
			if (nodes.get(i).isReady()) {
				queue.enqueue(i);
			}
		}

		while (!queue.isEmpty()) {
			int index = queue.dequeue();
			LightPropagationNode node = nodes.get(index);
			if (!node.tryRun()) {
				continue;
			}

			for (int i = 0; i < nodes.size(); i++) {
				LightPropagationNode dependent = nodes.get(i);
				dependent.dependencyCompleted(node);
				if (dependent.isReady()) {
					queue.enqueue(i);
				}
			}
		}
	}

	public void clear() {
		nodes.clear();
	}

	public static final class LightPropagationNode {
		private final int sectionIndex;
		private final Runnable propagation;
		private int pendingDependencies;
		private boolean completed;

		private LightPropagationNode(int sectionIndex, Runnable propagation) {
			this.sectionIndex = sectionIndex;
			this.propagation = propagation;
		}

		public void addDependency(LightPropagationNode dependency) {
			pendingDependencies++;
		}

		public void dependencyCompleted(LightPropagationNode completedNode) {
			if (pendingDependencies > 0) {
				pendingDependencies--;
			}
		}

		public boolean isReady() {
			return !completed && pendingDependencies == 0;
		}

		public boolean tryRun() {
			if (completed || pendingDependencies > 0) {
				return false;
			}
			propagation.run();
			completed = true;
			return true;
		}

		public int sectionIndex() {
			return sectionIndex;
		}
	}
}
