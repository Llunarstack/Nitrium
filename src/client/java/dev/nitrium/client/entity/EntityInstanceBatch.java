package dev.nitrium.client.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Groups identical entity types so they can be drawn in one instanced pass. The packed transforms
 * feed {@link EntityTransformBuffer}; the instanced draw itself isn't implemented yet.
 */
public final class EntityInstanceBatch {
	private final Map<String, List<InstanceData>> batches = new HashMap<>();

	public void clear() {
		batches.clear();
	}

	public void add(Entity entity, Matrix4f transform, int animationFrame) {
		String key = entity.getType().toString();
		batches.computeIfAbsent(key, ignored -> new ArrayList<>())
				.add(new InstanceData(entity.getId(), transform, animationFrame, entity.position()));
	}

	public Map<String, List<InstanceData>> batches() {
		return batches;
	}

	public int totalInstances() {
		return batches.values().stream().mapToInt(List::size).sum();
	}

	public record InstanceData(int entityId, Matrix4f transform, int animationFrame, Vec3 position) {
	}
}
