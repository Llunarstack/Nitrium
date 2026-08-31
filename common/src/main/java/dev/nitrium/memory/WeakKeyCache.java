package dev.nitrium.memory;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Cache with weak keys so world/chunk scoped objects are GC-eligible after unload.
 */
public final class WeakKeyCache<K, V> {
	private final Map<WeakKey<K>, V> backing = new ConcurrentHashMap<>();

	public V get(K key, Function<K, V> factory) {
		purgeStale();
		WeakKey<K> weakKey = new WeakKey<>(key);
		return backing.computeIfAbsent(weakKey, ignored -> factory.apply(key));
	}

	public void clear() {
		backing.clear();
	}

	public void purgeStale() {
		backing.keySet().removeIf(reference -> reference.get() == null);
	}

	public int size() {
		purgeStale();
		return backing.size();
	}

	private static final class WeakKey<K> extends WeakReference<K> {
		private final int hash;

		private WeakKey(K referent) {
			super(referent);
			this.hash = referent.hashCode();
		}

		@Override
		public boolean equals(Object object) {
			if (this == object) {
				return true;
			}
			if (!(object instanceof WeakKey<?> other)) {
				return false;
			}

			Object left = get();
			Object right = other.get();
			return left != null && left.equals(right);
		}

		@Override
		public int hashCode() {
			return hash;
		}
	}
}
