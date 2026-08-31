package dev.nitrium.layout;

import java.util.HashMap;
import java.util.Map;

/**
 * Deduplicates registry object references to reduce heap footprint.
 */
public final class RegistryDeduplicator {
	private final Map<Integer, Integer> canonicalIds = new HashMap<>();
	private int deduplicated;

	public int canonicalize(int registryId) {
		Integer existing = canonicalIds.putIfAbsent(registryId, registryId);
		if (existing != null && !existing.equals(registryId)) {
			deduplicated++;
			return existing;
		}
		return registryId;
	}

	public int deduplicatedCount() {
		return deduplicated;
	}

	public void clear() {
		canonicalIds.clear();
		deduplicated = 0;
	}
}
