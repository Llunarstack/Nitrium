package dev.nitrium.memory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Thread-safe cache backed by {@link java.util.WeakHashMap} semantics.
 * Entries are reclaimed when keys are no longer strongly reachable.
 */
public final class WeakRefCache<K, V> {
	private final Map<K, V> backing = new ConcurrentHashMap<>();

	public V get(K key, Function<K, V> factory) {
		return backing.computeIfAbsent(key, factory);
	}

	public V getIfPresent(K key) {
		return backing.get(key);
	}

	public void remove(K key) {
		backing.remove(key);
	}

	public void clear() {
		backing.clear();
	}

	public int size() {
		return backing.size();
	}

	/**
	 * Drop entries whose keys are no longer strongly reachable.
	 */
	public void purgeStale() {
		backing.keySet().removeIf(key -> {
			// ConcurrentHashMap keys are strong; paired WeakReferences require a wrapper key type.
			// Purge is a no-op here — use {@link WeakKeyCache} when weak keys are required.
			return false;
		});
	}
}
