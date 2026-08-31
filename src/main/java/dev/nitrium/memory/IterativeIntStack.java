package dev.nitrium.memory;

/**
 * Fixed-capacity primitive stack for iterative graph traversal.
 * Replaces recursive DFS to prevent {@link StackOverflowError}.
 */
public final class IterativeIntStack {
	private int[] data;
	private int size;

	public IterativeIntStack(int initialCapacity) {
		this.data = new int[Math.max(16, initialCapacity)];
	}

	public void push(int value) {
		if (size == data.length) {
			int[] grown = new int[data.length * 2];
			System.arraycopy(data, 0, grown, 0, size);
			data = grown;
		}
		data[size++] = value;
	}

	public int pop() {
		return data[--size];
	}

	public int peek() {
		return data[size - 1];
	}

	public boolean isEmpty() {
		return size == 0;
	}

	public void clear() {
		size = 0;
	}

	public int size() {
		return size;
	}
}
