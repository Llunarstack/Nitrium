package dev.nitrium.client.gui;

/**
 * GUI render engine profiling counters.
 */
public final class GuiStats {
	private long hudRebuilds;
	private long hudInvalidations;

	public void recordHudRebuild() {
		hudRebuilds++;
	}

	public void recordHudInvalidation() {
		hudInvalidations++;
	}

	public void reset() {
		hudRebuilds = 0;
		hudInvalidations = 0;
	}

	public long hudRebuilds() {
		return hudRebuilds;
	}

	public long hudInvalidations() {
		return hudInvalidations;
	}
}
