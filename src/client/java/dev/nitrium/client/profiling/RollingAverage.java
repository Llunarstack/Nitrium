package dev.nitrium.client.profiling;

public final class RollingAverage {
	private final double[] samples;
	private int index;
	private int count;
	private double sum;

	public RollingAverage(int capacity) {
		this.samples = new double[Math.max(1, capacity)];
	}

	public void add(double value) {
		if (count == samples.length) {
			sum -= samples[index];
		} else {
			count++;
		}

		samples[index] = value;
		sum += value;
		index = (index + 1) % samples.length;
	}

	public double average() {
		return count == 0 ? 0.0 : sum / count;
	}

	public int sampleCount() {
		return count;
	}

	public void reset() {
		index = 0;
		count = 0;
		sum = 0.0;
	}
}
