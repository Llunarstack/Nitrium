package dev.nitrium.worldgen.pool;

import java.util.Arrays;

/**
 * Thread-local primitive array pool — zero allocation inside hot worldgen loops.
 */
public final class PrimitiveBufferPool {
	private float[] floats = new float[0];
	private int[] ints = new int[0];
	private long[] longs = new long[0];
	private byte[] bytes = new byte[0];

	public float[] borrowFloats(int size) {
		if (floats.length < size) {
			floats = new float[size];
		} else {
			Arrays.fill(floats, 0, size, 0.0f);
		}
		return floats;
	}

	public int[] borrowInts(int size) {
		if (ints.length < size) {
			ints = new int[size];
		} else {
			Arrays.fill(ints, 0, size, 0);
		}
		return ints;
	}

	public long[] borrowLongs(int size) {
		if (longs.length < size) {
			longs = new long[size];
		} else {
			Arrays.fill(longs, 0, size, 0L);
		}
		return longs;
	}

	public byte[] borrowBytes(int size) {
		if (bytes.length < size) {
			bytes = new byte[size];
		} else {
			Arrays.fill(bytes, 0, size, (byte) 0);
		}
		return bytes;
	}
}
