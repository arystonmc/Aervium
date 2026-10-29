package com.aryston.kinetic.mixin;

import com.aryston.kinetic.world.spawn.OwnerTerrainWait;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
abstract class ServerGamePacketListenerTerrainWaitMixin {
    @Shadow
    public ServerPlayer player;

    @Inject(method = "tickClientLoadTimeout", at = @At("HEAD"), cancellable = true)
    private void kinetic$waitForOwnerTerrain(CallbackInfo callback) {
        if (OwnerTerrainWait.holding() && player.level().getServer().isSingleplayerOwner(player.nameAndId())) {
            callback.cancel();
        }
    }
}
