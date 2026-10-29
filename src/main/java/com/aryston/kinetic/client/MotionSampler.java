package com.aryston.kinetic.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.TickRateManager;

final class MotionSampler {
    static final double NOT_SAMPLED = Double.NaN;

    private static final float DEFAULT_MILLIS_PER_TICK = 50.0F;

    private MotionSampler() {
    }

    static double simulatedFrameMillis(Minecraft minecraft, DeltaTracker deltaTracker) {
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.isPaused()) {
            return NOT_SAMPLED;
        }
        TickRateManager tickRate = level.tickRateManager();
        if (!tickRate.runsNormally()) {
            return NOT_SAMPLED;
        }
        return deltaTracker.getGameTimeDeltaTicks() * (double) Math.max(DEFAULT_MILLIS_PER_TICK, tickRate.millisecondsPerTick());
    }
}
