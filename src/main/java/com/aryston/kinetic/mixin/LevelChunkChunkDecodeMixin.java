package com.aryston.kinetic.mixin;

import com.aryston.kinetic.client.chunk.ChunkPacketDecoding;
import com.aryston.kinetic.client.chunk.DecodedChunk;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelChunk.class)
abstract class LevelChunkChunkDecodeMixin {
    @Inject(method = "replaceWithPacketData", at = @At("HEAD"))
    private void kinetic$takeDecodedSections(int chunkX, int chunkZ, ClientboundLevelChunkPacketData data, CallbackInfo callback) {
        DecodedChunk decoded = ChunkPacketDecoding.applying();
        if (decoded != null) {
            LevelChunkSection[] sections = ((LevelChunk) (Object) this).getSections();
            System.arraycopy(decoded.sections(), 0, sections, 0, sections.length);
        }
    }

    @WrapOperation(
        method = "replaceWithPacketData",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/LevelChunkSection;read(Lnet/minecraft/network/FriendlyByteBuf;)V")
    )
    private void kinetic$skipDecodedRead(LevelChunkSection section, FriendlyByteBuf buffer, Operation<Void> original) {
        if (ChunkPacketDecoding.applying() == null) {
            original.call(section, buffer);
        }
    }

    @WrapOperation(method = "replaceWithPacketData", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/LevelChunk;initializeLightSources()V"))
    private void kinetic$takeDecodedLightSources(LevelChunk chunk, Operation<Void> original) {
        DecodedChunk decoded = ChunkPacketDecoding.applying();
        if (decoded == null) {
            original.call(chunk);
            return;
        }
        ((ChunkAccessLightSourcesAccessor) chunk).kinetic$setSkyLightSources(decoded.skyLightSources());
    }
}
