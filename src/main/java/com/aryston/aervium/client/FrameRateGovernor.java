package com.aryston.aervium.client;

import com.aryston.aervium.core.budget.FrameRatePolicy;
import com.aryston.aervium.core.budget.FrameRatePolicy.WindowState;
import net.minecraft.client.Minecraft;

public final class FrameRateGovernor {
    private static volatile FrameRatePolicy policy = new FrameRatePolicy(false, 0, 0);
    private static volatile boolean suspended;

    private FrameRateGovernor() {
    }

    public static int limit(int vanillaLimit) {
        if (suspended) {
            return vanillaLimit;
        }
        return policy.limit(vanillaLimit, windowState(Minecraft.getInstance()));
    }

    static void apply(FrameRatePolicy newPolicy) {
        policy = newPolicy;
    }

    public static void suspend(boolean suspend) {
        suspended = suspend;
    }

    private static WindowState windowState(Minecraft minecraft) {
        if (!minecraft.getWindow().isFocused()) {
            return WindowState.UNFOCUSED;
        }
        return minecraft.isPaused() ? WindowState.PAUSED : WindowState.ACTIVE;
    }
}
