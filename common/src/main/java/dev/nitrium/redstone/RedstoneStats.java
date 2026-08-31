package dev.nitrium.redstone;

/**
 * Redstone optimization profiling counters.
 */
public final class RedstoneStats {
	private long wireUpdates;
	private long hopperTicksSkipped;
	private long furnaceTicksSkipped;

	public void recordWireUpdate() {
		wireUpdates++;
	}

	public void recordHopperTickSkipped() {
		hopperTicksSkipped++;
	}

	public void recordFurnaceTickSkipped() {
		furnaceTicksSkipped++;
	}

	public void reset() {
		wireUpdates = 0;
		hopperTicksSkipped = 0;
		furnaceTicksSkipped = 0;
	}

	public long wireUpdates() {
		return wireUpdates;
	}

	public long hopperTicksSkipped() {
		return hopperTicksSkipped;
	}

	public long furnaceTicksSkipped() {
		return furnaceTicksSkipped;
	}
}
