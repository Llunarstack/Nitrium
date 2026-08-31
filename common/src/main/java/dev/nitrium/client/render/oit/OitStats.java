package dev.nitrium.client.render.oit;

/**
 * OIT pipeline profiling counters.
 */
public final class OitStats {
	private long frames;
	private long composites;

	public void recordFrame() {
		frames++;
	}

	public void recordComposite() {
		composites++;
	}

	public void reset() {
		frames = 0;
		composites = 0;
	}

	public long frames() {
		return frames;
	}

	public long composites() {
		return composites;
	}
}
