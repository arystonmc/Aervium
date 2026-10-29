package com.aryston.kinetic.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(IntegratedServer.class)
abstract class IntegratedServerJoinDistanceMixin {
    private static final int MIN_DISTANCE = 2;

    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    private int previousSimulationDistance;

    @Inject(method = "initServer", at = @At("RETURN"))
    private void kinetic$useClientDistancesBeforeJoin(CallbackInfoReturnable<Boolean> callback) {
        PlayerList players = ((IntegratedServer) (Object) this).getPlayerList();
        players.setViewDistance(Math.max(MIN_DISTANCE, minecraft.options.renderDistance().get()));
        previousSimulationDistance = Math.max(MIN_DISTANCE, minecraft.options.simulationDistance().get());
        players.setSimulationDistance(previousSimulationDistance);
    }
}
