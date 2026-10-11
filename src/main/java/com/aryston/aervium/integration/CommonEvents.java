package com.aryston.aervium.integration;

import com.aryston.aervium.world.TickTimeRecorder;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class CommonEvents {
    private CommonEvents() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(CommonEvents::onServerTickStart);
        NeoForge.EVENT_BUS.addListener(CommonEvents::onServerTickEnd);
        NeoForge.EVENT_BUS.addListener(CommonEvents::onServerStopped);
    }

    private static void onServerTickStart(ServerTickEvent.Pre event) {
        TickTimeRecorder.SERVER.tickStarted();
    }

    private static void onServerTickEnd(ServerTickEvent.Post event) {
        TickTimeRecorder.SERVER.tickFinished();
    }

    private static void onServerStopped(ServerStoppedEvent event) {
        TickTimeRecorder.SERVER.serverStopped();
    }
}
