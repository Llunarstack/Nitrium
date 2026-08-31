package dev.nitrium.client.nativegl;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/**
 * Extracts 6 frustum planes from the active camera for native SIMD culling.
 */
public final class FrustumPlaneExtractor {
	private FrustumPlaneExtractor() {
	}

	public static float[] extractPlanes() {
		Minecraft client = Minecraft.getInstance();
		Camera camera = client.gameRenderer.getMainCamera();
		float partialTick = client.getDeltaTracker().getGameTimeDeltaPartialTick(false);

		Matrix4f projection = new Matrix4f(client.gameRenderer.getProjectionMatrix(partialTick));

		Matrix4f view = new Matrix4f();
		Quaternionf rotation = camera.rotation();
		view.rotate(rotation.conjugate(new Quaternionf()));
		view.translate(
				-(float) camera.position().x,
				-(float) camera.position().y,
				-(float) camera.position().z
		);

		Matrix4f clip = projection.mul(view, new Matrix4f());
		return extractPlanesFromClipMatrix(clip);
	}

	private static float[] extractPlanesFromClipMatrix(Matrix4f matrix) {
		float[] planes = new float[24];
		float[] m = new float[16];
		matrix.get(m);

		// Gribb-Hartmann planes from clip matrix
		addPlane(planes, 0, m[3] + m[0], m[7] + m[4], m[11] + m[8], m[15] + m[12]);
		addPlane(planes, 1, m[3] - m[0], m[7] - m[4], m[11] - m[8], m[15] - m[12]);
		addPlane(planes, 2, m[3] + m[1], m[7] + m[5], m[11] + m[9], m[15] + m[13]);
		addPlane(planes, 3, m[3] - m[1], m[7] - m[5], m[11] - m[9], m[15] - m[13]);
		addPlane(planes, 4, m[3] + m[2], m[7] + m[6], m[11] + m[10], m[15] + m[14]);
		addPlane(planes, 5, m[3] - m[2], m[7] - m[6], m[11] - m[10], m[15] - m[14]);
		return planes;
	}

	private static void addPlane(float[] planes, int index, float a, float b, float c, float d) {
		float length = (float) Math.sqrt(a * a + b * b + c * c);
		if (length > 0.0f) {
			a /= length;
			b /= length;
			c /= length;
			d /= length;
		}
		planes[index * 4] = a;
		planes[index * 4 + 1] = b;
		planes[index * 4 + 2] = c;
		planes[index * 4 + 3] = d;
	}
}
