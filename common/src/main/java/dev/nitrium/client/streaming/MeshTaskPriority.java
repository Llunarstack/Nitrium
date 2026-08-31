package dev.nitrium.client.streaming;

/**
 * Velocity-weighted priority for streaming section meshes. Two factors multiply: a smooth
 * {@code 1/(1+(d/64)^2)} distance falloff (no singularity at the camera, graceful out to the render
 * edge), and — when the camera is moving — a forward-cone bias that boosts sections ahead of travel
 * and damps those behind, though never to zero so turning around isn't starved. Higher = build
 * sooner; the scheduler is a max-heap on this value.
 */
public final class MeshTaskPriority {
	/** Distance (blocks) at which the falloff term reaches 0.5. Tuned to ~4 chunks. */
	private static final double FALLOFF_SCALE_BLOCKS = 64.0;

	/** Minimum forward-cone weight so sections directly behind the camera still stream. */
	private static final double MIN_CONE_WEIGHT = 0.2;

	private MeshTaskPriority() {
	}

	public static double compute(
			double cameraX,
			double cameraY,
			double cameraZ,
			double velocityX,
			double velocityY,
			double velocityZ,
			SectionKey section
	) {
		double toX = section.centerX() - cameraX;
		double toY = section.centerY() - cameraY;
		double toZ = section.centerZ() - cameraZ;
		double distanceSq = toX * toX + toY * toY + toZ * toZ;
		double distance = Math.sqrt(distanceSq);

		double normalized = distance / FALLOFF_SCALE_BLOCKS;
		double distanceWeight = 1.0 / (1.0 + normalized * normalized);

		double velocityLength = Math.sqrt(velocityX * velocityX + velocityY * velocityY + velocityZ * velocityZ);
		if (velocityLength <= 1.0E-4 || distance <= 1.0E-4) {
			return distanceWeight; // stationary or at-camera: distance only
		}

		double alignment = (velocityX * toX + velocityY * toY + velocityZ * toZ) / (velocityLength * distance);
		// Map alignment [-1, 1] -> cone weight [MIN_CONE_WEIGHT, 1]; square the forward half so the
		// leading cone is sharply favoured over the flanks.
		double forward = Math.max(0.0, alignment);
		double coneWeight = MIN_CONE_WEIGHT + (1.0 - MIN_CONE_WEIGHT) * forward * forward;

		return distanceWeight * coneWeight;
	}
}
