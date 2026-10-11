package com.aryston.aervium.client;

import com.aryston.aervium.Aervium;
import com.aryston.aervium.client.benchmark.BenchmarkDriver;
import com.aryston.aervium.client.pipeline.PipelineWarmup;
import com.aryston.aervium.client.pipeline.ShaderCaches;
import com.aryston.aervium.config.AerviumConfig;
import com.aryston.aervium.core.budget.SystemTimings;
import com.aryston.aervium.core.budget.TimedSystem;
import com.aryston.aervium.integration.CompatibilityGuard;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.GameShuttingDownEvent;

final class ClientEvents {
    private static final String CACHE_FOLDER = "aervium/cache";
    private static final Identifier OVERLAY_LAYER = Identifier.fromNamespaceAndPath(Aervium.MOD_ID, "telemetry");
    private static final List<String> PASSIVE_RENDERER_MODS = CompatibilityGuard.loadedRendererMods();
    private static final ClientTelemetry TELEMETRY = new ClientTelemetry();
    private static final TelemetryOverlay OVERLAY = new TelemetryOverlay(TELEMETRY, false, AerviumConfig.defaultFrameBudget());
    private static final Optional<BenchmarkDriver> BENCHMARK = BenchmarkDriver.requested(
        TELEMETRY.gcPauses(),
        TELEMETRY.jobs(),
        TELEMETRY.mainThread(),
        VulkanMemoryProbe::sample
    );

    private static long frameStart;
    private static long clientTickStart;
    private static boolean startupReported;

    private ClientEvents() {
    }

    static void register(IEventBus modBus) {
        if (PASSIVE_RENDERER_MODS.isEmpty()) {
            ShaderCaches.install(FMLPaths.GAMEDIR.get().resolve(CACHE_FOLDER), TELEMETRY.jobs());
            PipelineWarmup.install(TELEMETRY.jobs(), TELEMETRY.mainThread());
        }
        modBus.addListener(ClientEvents::onClientSetup);
        modBus.addListener(ClientEvents::onConfigLoading);
        modBus.addListener(ClientEvents::onConfigReloading);
        modBus.addListener(AerviumKeys::register);
        modBus.addListener(ClientEvents::onRegisterGuiLayers);
        NeoForge.EVENT_BUS.addListener(ClientEvents::onFrameStart);
        NeoForge.EVENT_BUS.addListener(ClientEvents::onFrameEnd);
        NeoForge.EVENT_BUS.addListener(ClientEvents::onClientTickStart);
        NeoForge.EVENT_BUS.addListener(ClientEvents::onClientTickEnd);
        NeoForge.EVENT_BUS.addListener(ClientEvents::onLoggingIn);
        NeoForge.EVENT_BUS.addListener(ClientEvents::onShutdown);
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        if (!PASSIVE_RENDERER_MODS.isEmpty()) {
            Aervium.LOGGER.warn("Aervium keeps its optimizations off because these renderer mods are installed: {}", PASSIVE_RENDERER_MODS);
        }
        if (BENCHMARK.isPresent()) {
            Aervium.LOGGER.info("Aervium benchmark requested; it starts when the title screen is shown");
        }
    }

    private static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(OVERLAY_LAYER, OVERLAY);
    }

    private static void onConfigLoading(ModConfigEvent.Loading event) {
        applyConfig();
    }

    private static void onConfigReloading(ModConfigEvent.Reloading event) {
        applyConfig();
    }

    private static void applyConfig() {
        OVERLAY.setVisible(AerviumConfig.OVERLAY_VISIBLE.getAsBoolean());
        OVERLAY.setBudget(AerviumConfig.frameBudget());
        if (PASSIVE_RENDERER_MODS.isEmpty()) {
            FrameRateGovernor.apply(AerviumConfig.frameRatePolicy());
        }
    }

    private static void onFrameStart(RenderFrameEvent.Pre event) {
        long now = System.nanoTime();
        frameStart = now;
        TELEMETRY.frameStarted(now);
        if (BENCHMARK.isPresent()) {
            BENCHMARK.get().frameStarted(now);
        }
    }

    private static void onFrameEnd(RenderFrameEvent.Post event) {
        SystemTimings.SHARED.end(TimedSystem.FRAME, frameStart);
        TELEMETRY.frameFinished();
    }

    private static void onClientTickStart(ClientTickEvent.Pre event) {
        clientTickStart = SystemTimings.begin();
    }

    private static void onClientTickEnd(ClientTickEvent.Post event) {
        SystemTimings.SHARED.end(TimedSystem.CLIENT_TICK, clientTickStart);
        Minecraft minecraft = Minecraft.getInstance();
        if (!startupReported && minecraft.isGameLoadFinished()) {
            startupReported = true;
            ShaderCaches.reportStartup();
        }
        while (AerviumKeys.TOGGLE_OVERLAY.consumeClick()) {
            toggleOverlay();
        }
        if (OVERLAY.isVisible()) {
            TELEMETRY.clientTick(OVERLAY.budget(), snapshot -> OVERLAY.publish(snapshot, PASSIVE_RENDERER_MODS));
        }
        BENCHMARK.ifPresent(benchmark -> benchmark.clientTick(minecraft));
    }

    private static void toggleOverlay() {
        boolean visible = !OVERLAY.isVisible();
        OVERLAY.setVisible(visible);
        AerviumConfig.OVERLAY_VISIBLE.set(visible);
        AerviumConfig.OVERLAY_VISIBLE.save();
    }

    private static void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        TELEMETRY.resetFrames();
    }

    private static void onShutdown(GameShuttingDownEvent event) {
        ShaderCaches.shutdown();
        TELEMETRY.close();
    }
}
