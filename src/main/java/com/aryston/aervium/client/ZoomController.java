package com.aryston.aervium.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.CalculatePlayerTurnEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;

final class ZoomController {
    private static final float DEFAULT_ZOOM = 4.0F;
    private static final float MIN_ZOOM = 1.2F;
    private static final float MAX_ZOOM = 30.0F;
    private static final float ZOOM_STEP = 1.25F;
    private static final float SMOOTHING_SPEED = 20.0F;
    private static final float SNAP_EPSILON = 0.001F;

    private static float targetZoom = DEFAULT_ZOOM;
    private static float currentZoom = 1.0F;
    private static long lastFrameNanos;
    private static boolean worldFov = true;

    private ZoomController() {
    }

    static void register() {
        NeoForge.EVENT_BUS.addListener(ZoomController::onFrameStart);
        NeoForge.EVENT_BUS.addListener(ZoomController::onComputeFov);
        NeoForge.EVENT_BUS.addListener(ZoomController::onMouseScroll);
        NeoForge.EVENT_BUS.addListener(ZoomController::onCalculatePlayerTurn);
    }

    private static void onFrameStart(RenderFrameEvent.Pre event) {
        worldFov = true;
        long now = System.nanoTime();
        if (lastFrameNanos == 0L) {
            lastFrameNanos = now;
            return;
        }
        float deltaSeconds = Math.min((now - lastFrameNanos) / 1_000_000_000.0F, 0.1F);
        lastFrameNanos = now;

        Minecraft minecraft = Minecraft.getInstance();
        boolean active = isZoomActive(minecraft);
        float target = active ? targetZoom : 1.0F;

        if (Math.abs(currentZoom - target) < SNAP_EPSILON) {
            currentZoom = target;
        } else {
            float blend = 1.0F - (float) Math.exp(-deltaSeconds * SMOOTHING_SPEED);
            currentZoom += (target - currentZoom) * blend;
        }

        if (!active && currentZoom <= 1.001F) {
            targetZoom = DEFAULT_ZOOM;
        }
    }

    private static boolean isZoomActive(Minecraft minecraft) {
        return minecraft.gui.screen() == null && AerviumKeys.ZOOM.isDown();
    }

    private static void onComputeFov(ViewportEvent.ComputeFov event) {
        if (worldFov) {
            worldFov = false;
            if (currentZoom > 1.001F) {
                event.setFOV(event.getFOV() / currentZoom);
            }
        }
    }

    private static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!isZoomActive(minecraft)) {
            return;
        }
        double scroll = event.getScrollDeltaY();
        if (scroll == 0.0) {
            return;
        }
        event.setCanceled(true);
        if (scroll > 0.0) {
            targetZoom = Mth.clamp(targetZoom * ZOOM_STEP, MIN_ZOOM, MAX_ZOOM);
        } else {
            targetZoom = Mth.clamp(targetZoom / ZOOM_STEP, MIN_ZOOM, MAX_ZOOM);
        }
    }

    private static void onCalculatePlayerTurn(CalculatePlayerTurnEvent event) {
        if (currentZoom > 1.001F) {
            event.setMouseSensitivity(event.getMouseSensitivity() / currentZoom);
        }
    }
}

