package dev.nitrium.mixin;

import dev.nitrium.config.NitriumConfigManager;
import dev.nitrium.storage.AsyncChunkStorageEngine;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.SimpleRegionStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Mirrors chunk writes into Nitrium's async ring-buffer queue for off-tick draining.
 */
@Mixin(SimpleRegionStorage.class)
public abstract class SimpleRegionStorageMixin {
	@Inject(
			method = "write(Lnet/minecraft/world/level/ChunkPos;Ljava/util/function/Supplier;)Ljava/util/concurrent/CompletableFuture;",
			at = @At("HEAD"),
			require = 0
	)
	private void nitrium$enqueueAsyncSave(
			ChunkPos chunkPos,
			Supplier<CompoundTag> supplier,
			CallbackInfoReturnable<CompletableFuture<Void>> cir
	) {
		if (!NitriumConfigManager.get().enableAsyncChunkStorage) {
			return;
		}

		AsyncChunkStorageEngine engine = AsyncChunkStorageEngine.get();
		if (engine == null) {
			return;
		}

		try {
			CompoundTag tag = supplier.get();
			if (tag == null) {
				return;
			}

			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			NbtIo.writeCompressed(tag, stream);
			engine.enqueueChunkSave(chunkPos, stream.toByteArray());
		} catch (Exception ignored) {
			// Fall back to vanilla write only.
		}
	}
}
