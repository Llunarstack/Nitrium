package dev.nitrium.client.nativegl;

import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.textures.GpuTexture;
import org.jspecify.annotations.Nullable;

/**
 * Bridges Blaze3D {@link GpuTexture} handles to OpenGL texture ids for Nitrium's legacy GL paths.
 */
public final class GpuTextureBridge {
	private GpuTextureBridge() {
	}

	public static int glId(@Nullable GpuTexture texture) {
		if (texture instanceof GlTexture glTexture) {
			return glTexture.glId();
		}
		return 0;
	}
}
