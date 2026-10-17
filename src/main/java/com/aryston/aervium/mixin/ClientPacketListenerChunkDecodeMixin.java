package com.aryston.aervium.mixin;

import com.aryston.aervium.client.chunk.ChunkPacketDecoding;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
abstract class ClientPacketListenerChunkDecodeMixin {
    @Shadow
    private ClientLevel level;

    @Inject(method = "handleBundlePacket", at = @At("HEAD"))
    private void aervium$decodeBundledAhead(ClientboundBundlePacket packet, CallbackInfo callback) {
        if (Minecraft.getInstance().isSameThread()) {
            return;
        }
        for (Packet<? super ClientGamePacketListener> subPacket : packet.subPackets()) {
            if (subPacket instanceof ClientboundLevelChunkWithLightPacket chunk) {
                ChunkPacketDecoding.start(level, chunk.chunkData(), chunk.x(), chunk.z());
            }
        }
    }

    @Inject(method = "handleLevelChunkWithLight", at = @At("HEAD"))
    private void aervium$decodeAhead(ClientboundLevelChunkWithLightPacket packet, CallbackInfo callback) {
        if (!Minecraft.getInstance().isSameThread()) {
            ChunkPacketDecoding.start(level, packet.chunkData(), packet.x(), packet.z());
        }
    }
}
