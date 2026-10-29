package com.aryston.kinetic.client.benchmark;

import com.aryston.kinetic.core.telemetry.Durations;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;

final class JoinShots {
    private static final String PROPERTY = "kinetic.joinCheck.shots";
    private static final long LOADING_INTERVAL_NANOS = TimeUnit.MILLISECONDS.toNanos(700L);
    private static final long ENTRY_INTERVAL_NANOS = TimeUnit.MILLISECONDS.toNanos(70L);
    private static final long ENTRY_WINDOW_NANOS = TimeUnit.MILLISECONDS.toNanos(1400L);
    private static final int FULL_SIZE = 1;

    private final boolean enabled = Boolean.getBoolean(PROPERTY);
    private long nextShot;

    void loading(Minecraft minecraft, long now, long sinceStart, String phase) {
        if (!enabled || now < nextShot) {
            return;
        }
        nextShot = now + LOADING_INTERVAL_NANOS;
        grab(minecraft, String.format(Locale.ROOT, "join_%05d_%s", Math.round(Durations.millis(sinceStart)), phase.replace(' ', '_')));
    }

    void entered(long now) {
        nextShot = now;
    }

    void inGame(Minecraft minecraft, long now, long sinceEntry) {
        if (!enabled || sinceEntry > ENTRY_WINDOW_NANOS || now < nextShot) {
            return;
        }
        nextShot = now + ENTRY_INTERVAL_NANOS;
        grab(minecraft, String.format(Locale.ROOT, "join_entry_%04d", Math.round(Durations.millis(sinceEntry))));
    }

    private static void grab(Minecraft minecraft, String name) {
        Screenshot.grab(minecraft.gameDirectory, name + ".png", minecraft.gameRenderer.mainRenderTarget(), FULL_SIZE, message -> { });
    }
}
