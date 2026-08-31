package dev.nitrium.memory;

/**
 * Ring-buffer FIFO queue for iterative BFS traversal without recursion.
 */
public final class IterativeIntQueue {
	private int[] data;
	private int head;
	private int tail;
	private int count;

	public IterativeIntQueue(int initialCapacity) {
		this.data = new int[Math.max(16, initialCapacity)];
	}

	public void enqueue(int value) {
		ensureCapacity();
		data[tail] = value;
		tail = (tail + 1) % data.length;
		count++;
	}

	public int dequeue() {
		int value = data[head];
		head = (head + 1) % data.length;
		count--;
		return value;
	}

	public boolean isEmpty() {
		return count == 0;
	}

	public void clear() {
		head = 0;
		tail = 0;
		count = 0;
	}

	private void ensureCapacity() {
		if (count < data.length) {
			return;
		}

		int[] grown = new int[data.length * 2];
		for (int i = 0; i < count; i++) {
			grown[i] = data[(head + i) % data.length];
		}
		data = grown;
		head = 0;
		tail = count;
	}
}
