package dev.nitrium.client.culling.foliage;

import dev.nitrium.config.NitriumConfigManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Distance-adaptive foliage rendering policy.
 * <p>
 * Beyond {@code foliageOpaqueDistanceBlocks}, leaf blocks switch from translucent blending
 * to alpha-tested opaque rendering so they participate in early-Z and Hi-Z rejection.
 */
public final class FoliageCullPolicy {
	public enum RenderMode {
		TRANSLUCENT,
		ALPHA_TEST_OPAQUE
	}

	public RenderMode modeFor(BlockState state, Vec3 cameraPos, BlockPos blockPos) {
		if (!NitriumConfigManager.get().enableFoliageOptimization) {
			return RenderMode.TRANSLUCENT;
		}

		if (!isFoliage(state)) {
			return RenderMode.TRANSLUCENT;
		}

		double dx = blockPos.getX() + 0.5 - cameraPos.x;
		double dy = blockPos.getY() + 0.5 - cameraPos.y;
		double dz = blockPos.getZ() + 0.5 - cameraPos.z;
		double distanceSq = dx * dx + dy * dy + dz * dz;
		int threshold = NitriumConfigManager.get().foliageOpaqueDistanceBlocks;

		return distanceSq > (long) threshold * threshold ? RenderMode.ALPHA_TEST_OPAQUE : RenderMode.TRANSLUCENT;
	}

	public boolean shouldOptimize(BlockState state, Vec3 cameraPos, BlockPos blockPos) {
		return modeFor(state, cameraPos, blockPos) == RenderMode.ALPHA_TEST_OPAQUE;
	}

	private static boolean isFoliage(BlockState state) {
		return state.is(Blocks.OAK_LEAVES)
				|| state.is(Blocks.SPRUCE_LEAVES)
				|| state.is(Blocks.BIRCH_LEAVES)
				|| state.is(Blocks.JUNGLE_LEAVES)
				|| state.is(Blocks.ACACIA_LEAVES)
				|| state.is(Blocks.DARK_OAK_LEAVES)
				|| state.is(Blocks.MANGROVE_LEAVES)
				|| state.is(Blocks.CHERRY_LEAVES)
				|| state.is(Blocks.AZALEA_LEAVES)
				|| state.is(Blocks.FLOWERING_AZALEA_LEAVES)
				|| state.is(Blocks.PALE_OAK_LEAVES);
	}
}
