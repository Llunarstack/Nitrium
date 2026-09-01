package dev.nitrium.client.platform;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Cross-loader world-render stage callbacks fired from {@link dev.nitrium.client.mixin.LevelRendererMixin}
 * and {@link dev.nitrium.client.mixin.ChunkSectionsToRenderMixin}. Fabric and NeoForge still expose the
 * same stages through {@link ClientEvents}; Forge relies on these mixins exclusively.
 */
public final class ClientRenderStages {
	private static final List<Runnable> RENDER_START = new CopyOnWriteArrayList<>();
	private static final List<Runnable> AFTER_OPAQUE = new CopyOnWriteArrayList<>();
	private static final List<Runnable> BEFORE_ENTITIES = new CopyOnWriteArrayList<>();
	private static final List<Runnable> AFTER_ENTITIES = new CopyOnWriteArrayList<>();
	private static final List<Runnable> BEFORE_TRANSLUCENT = new CopyOnWriteArrayList<>();
	private static final List<Runnable> AFTER_TRANSLUCENT = new CopyOnWriteArrayList<>();
	private static final List<Runnable> BEFORE_DEBUG = new CopyOnWriteArrayList<>();
	private static final List<Runnable> RENDER_END = new CopyOnWriteArrayList<>();

	private ClientRenderStages() {
	}

	public static void onRenderStart(Runnable callback) {
		RENDER_START.add(callback);
	}

	public static void onAfterOpaque(Runnable callback) {
		AFTER_OPAQUE.add(callback);
	}

	public static void onBeforeEntities(Runnable callback) {
		BEFORE_ENTITIES.add(callback);
	}

	public static void onAfterEntities(Runnable callback) {
		AFTER_ENTITIES.add(callback);
	}

	public static void onBeforeTranslucent(Runnable callback) {
		BEFORE_TRANSLUCENT.add(callback);
	}

	public static void onAfterTranslucent(Runnable callback) {
		AFTER_TRANSLUCENT.add(callback);
	}

	public static void onBeforeDebug(Runnable callback) {
		BEFORE_DEBUG.add(callback);
	}

	public static void onRenderEnd(Runnable callback) {
		RENDER_END.add(callback);
	}

	public static void fireRenderStart() {
		RENDER_START.forEach(Runnable::run);
	}

	public static void fireAfterOpaque() {
		AFTER_OPAQUE.forEach(Runnable::run);
	}

	public static void fireBeforeEntities() {
		BEFORE_ENTITIES.forEach(Runnable::run);
	}

	public static void fireAfterEntities() {
		AFTER_ENTITIES.forEach(Runnable::run);
	}

	public static void fireBeforeTranslucent() {
		BEFORE_TRANSLUCENT.forEach(Runnable::run);
	}

	public static void fireAfterTranslucent() {
		AFTER_TRANSLUCENT.forEach(Runnable::run);
	}

	public static void fireBeforeDebug() {
		BEFORE_DEBUG.forEach(Runnable::run);
	}

	public static void fireRenderEnd() {
		RENDER_END.forEach(Runnable::run);
	}
}
