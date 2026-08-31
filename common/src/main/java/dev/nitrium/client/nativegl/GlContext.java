package dev.nitrium.client.nativegl;

import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL;

/**
 * Guards against calling OpenGL before the context is usable.
 *
 * <p>Fabric client entrypoints ({@code onInitializeClient}) run on the render thread but
 * <em>before</em> {@code GL.createCapabilities()} has populated LWJGL's function pointers, so any
 * {@code glGen*}/{@code glCreate*} at init dereferences a null pointer and hard-crashes the JVM
 * ({@code EXCEPTION_ACCESS_VIOLATION} in {@code lwjgl_opengl.dll}). All Nitrium GL resource
 * allocation must be deferred until {@link #isReady()} returns {@code true}, which only happens
 * once rendering has begun.
 */
public final class GlContext {
	private GlContext() {
	}

	/**
	 * @return {@code true} only on the render thread once GL capabilities have been created
	 */
	public static boolean isReady() {
		if (!RenderSystem.isOnRenderThread()) {
			return false;
		}
		try {
			return GL.getCapabilities() != null;
		} catch (Throwable notInitialized) {
			// LWJGL throws IllegalStateException when no capabilities exist for this thread yet.
			return false;
		}
	}
}
