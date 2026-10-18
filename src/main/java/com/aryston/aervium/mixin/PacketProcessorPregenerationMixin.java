package com.aryston.aervium.mixin;

import com.aryston.aervium.client.pregeneration.JoinPregeneration;
import java.util.Queue;
import net.minecraft.client.Minecraft;
import net.minecraft.network.PacketProcessor;
import net.neoforged.neoforge.network.handling.QueuedPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PacketProcessor.class)
abstract class PacketProcessorPregenerationMixin {
    @Shadow
    @Final
    private Queue<QueuedPacket> packetsToBeHandled;

    @Shadow
    private boolean closed;

    @Inject(method = "processQueuedPackets", at = @At("HEAD"), cancellable = true)
    private void aervium$spreadPacketsOverFrames(CallbackInfo callback) {
        if (!JoinPregeneration.spreadsWork() || Minecraft.getInstance().packetProcessor() != (Object) this) {
            return;
        }
        callback.cancel();
        long deadline = System.nanoTime() + JoinPregeneration.PACKET_BUDGET_NANOS;
        while (!closed && !packetsToBeHandled.isEmpty() && System.nanoTime() < deadline) {
            packetsToBeHandled.poll().handle();
        }
    }
}
