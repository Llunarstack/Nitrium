package dev.nitrium.client.mixin;

import dev.nitrium.client.streaming.CompactSectionData;
import dev.nitrium.client.streaming.SectionKey;
import dev.nitrium.client.streaming.SectionSnapshotExtractor;
import dev.nitrium.client.streaming.StreamingChunkLoader;
import dev.nitrium.config.NitriumConfigManager;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelChunk.class)
public abstract class LevelChunkSnapshotMixin {
	@Shadow
	public abstract Level getLevel();

	@Inject(method = "setBlockState", at = @At("RETURN"), require = 0)
	private void nitrium$captureSectionSnapshot(
			BlockPos blockPos,
			BlockState blockState,
			int flags,
			CallbackInfoReturnable<BlockState> cir
	) {
		if (!NitriumConfigManager.get().enableSectionDiskCache) {
			return;
		}

		Level level = this.getLevel();
		if (!(level instanceof ClientLevel clientLevel)) {
			return;
		}

		StreamingChunkLoader loader = StreamingChunkLoader.get();
		if (loader == null) {
			return;
		}

		int sectionIndex = clientLevel.getSectionIndexFromSectionY(SectionPos.blockToSectionCoord(blockPos.getY()));
		if (sectionIndex < 0) {
			return;
		}

		LevelChunk chunk = (LevelChunk) (Object) this;
		LevelChunkSection section = chunk.getSection(sectionIndex);
		if (section == null || section.hasOnlyAir()) {
			return;
		}

		CompactSectionData data = SectionSnapshotExtractor.extract(section, SectionPos.sectionToBlockCoord(sectionIndex));
		if (data == null) {
			return;
		}

		Identifier dimension = clientLevel.dimension().identifier();
		SectionKey key = StreamingChunkLoader.key(
				dimension,
				chunk.getPos().x,
				SectionPos.sectionToBlockCoord(sectionIndex),
				chunk.getPos().z
		);
		loader.storeSection(key, data);
	}
}
