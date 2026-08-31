package dev.nitrium.client.streaming;

import dev.nitrium.Nitrium;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Extracts lock-free {@link CompactSectionData} snapshots from live chunk sections.
 * Called on the client thread during chunk unload or section update events.
 */
public final class SectionSnapshotExtractor {
	private SectionSnapshotExtractor() {
	}

	public static CompactSectionData extract(LevelChunkSection section, int sectionY) {
		Map<Integer, Integer> paletteLookup = new HashMap<>();
		List<Integer> palette = new ArrayList<>();
		byte[] indices = new byte[CompactSectionData.BLOCKS_PER_SECTION];

		int index = 0;
		for (int y = 0; y < 16; y++) {
			for (int z = 0; z < 16; z++) {
				for (int x = 0; x < 16; x++) {
					BlockState state = section.getBlockState(x, y, z);
					int stateId = Block.getId(state);

					Integer paletteIndex = paletteLookup.get(stateId);
					if (paletteIndex == null) {
						if (palette.size() >= 256) {
							Nitrium.LOGGER.debug("Section Y={} exceeded 256-entry palette during snapshot", sectionY);
							return null;
						}
						paletteIndex = palette.size();
						paletteLookup.put(stateId, paletteIndex);
						palette.add(stateId);
					}

					indices[index++] = paletteIndex.byteValue();
				}
			}
		}

		int[] paletteArray = palette.stream().mapToInt(Integer::intValue).toArray();
		return new CompactSectionData(sectionY, paletteArray, indices);
	}

	public static int sectionYFromIndex(int sectionIndex) {
		return SectionPos.sectionToBlockCoord(sectionIndex);
	}
}
