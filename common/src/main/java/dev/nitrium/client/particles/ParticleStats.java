package dev.nitrium.client.particles;

/**
 * GPU particle engine profiling counters.
 */
public final class ParticleStats {
	private long simulatePasses;
	private long drawCalls;
	private long particlesDrawn;

	public void recordSimulatePass() {
		simulatePasses++;
	}

	public void recordDraw(int count) {
		drawCalls++;
		particlesDrawn += count;
	}

	public void reset() {
		simulatePasses = 0;
		drawCalls = 0;
		particlesDrawn = 0;
	}

	public long simulatePasses() {
		return simulatePasses;
	}

	public long drawCalls() {
		return drawCalls;
	}

	public long particlesDrawn() {
		return particlesDrawn;
	}
}
