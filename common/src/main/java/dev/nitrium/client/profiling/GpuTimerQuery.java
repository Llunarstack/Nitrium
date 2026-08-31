package dev.nitrium.client.profiling;

import dev.nitrium.client.nativegl.GlContext;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL33;

/**
 * Single {@link GL33#GL_TIME_ELAPSED} query with one-frame latency, standard for GPU profiling.
 *
 * <p>The GL query object is allocated lazily on first use — never in the constructor — because
 * this type is created during client init, before the GL context is ready. See {@link GlContext}.
 */
public final class GpuTimerQuery {
	private int queryId;
	private boolean active;
	private boolean pending;

	public GpuTimerQuery() {
		// No GL here: the query is generated lazily in begin() once the context is ready.
	}

	private boolean ensureQuery() {
		if (queryId != 0) {
			return true;
		}
		if (!GlContext.isReady()) {
			return false;
		}
		queryId = GL15.glGenQueries();
		return queryId != 0;
	}

	public void begin() {
		if (pending || active || !ensureQuery()) {
			return;
		}

		GL15.glBeginQuery(GL33.GL_TIME_ELAPSED, queryId);
		active = true;
	}

	public void end() {
		if (!active) {
			return;
		}

		GL15.glEndQuery(GL33.GL_TIME_ELAPSED);
		active = false;
		pending = true;
	}

	/**
	 * @return elapsed GPU time in nanoseconds for the last completed query, or -1 if unavailable
	 */
	public long pollNanoseconds() {
		if (!pending || queryId == 0) {
			return -1L;
		}

		int available = GL33.glGetQueryObjecti(queryId, GL15.GL_QUERY_RESULT_AVAILABLE);
		if (available == 0) {
			return -1L;
		}

		long elapsedNs = GL33.glGetQueryObjecti64(queryId, GL15.GL_QUERY_RESULT);
		pending = false;
		return elapsedNs;
	}

	public void close() {
		if (queryId != 0 && GlContext.isReady()) {
			GL15.glDeleteQueries(queryId);
		}
		queryId = 0;
		active = false;
		pending = false;
	}
}
