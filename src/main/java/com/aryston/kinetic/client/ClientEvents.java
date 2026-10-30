package com.aryston.kinetic.client;

import com.aryston.kinetic.Kinetic;
import com.aryston.kinetic.client.benchmark.BenchmarkDriver;
import com.aryston.kinetic.client.benchmark.JoinCheck;
import com.aryston.kinetic.client.chunk.DirectDraws;
import com.aryston.kinetic.client.chunk.LeafCulling;
import com.aryston.kinetic.client.gpu.GpuCulling;
import com.aryston.kinetic.client.gpu.GpuStageTimer;
import com.aryston.kinetic.client.lod.DistantTerrain;
import com.aryston.kinetic.client.lod.DistantTerrainFog;
import com.aryston.kinetic.client.lod.DistantTerrainRenderer;
import com.aryston.kinetic.client.culling.TerrainOcclusion;
import com.aryston.kinetic.client.pipeline.PipelineWarmup;
import com.aryston.kinetic.client.pipeline.ShaderCaches;
import com.aryston.kinetic.client.pregeneration.JoinPregeneration;
import com.aryston.kinetic.config.KineticConfig;
import com.aryston.kinetic.core.budget.SystemTimings;
import com.aryston.kinetic.core.budget.TimedSystem;
import com.aryston.kinetic.core.module.OptimizationModule;
import com.aryston.kinetic.core.telemetry.FrameCostRecorder;
import com.aryston.kinetic.core.telemetry.GarbageCollector;
import com.aryston.kinetic.core.telemetry.GcAdvice;
import com.aryston.kinetic.core.telemetry.TelemetrySnapshot;
import com.aryston.kinetic.integration.CompatibilityGuard;
import com.aryston.kinetic.integration.MixinModules;
import com.aryston.kinetic.world.spawn.SpawnPregenerationSettings;
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
import net.neoforged.neoforge.client.event.FlipFrameEvent;
import net.neoforged.neoforge.client.event.RegisterDebugEntriesEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.GameShuttingDownEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;

final class ClientEvents {
    private static final String CACHE_FOLDER = "kinetic/cache";
    private static final Identifier OVERLAY_LAYER = Identifier.fromNamespaceAndPath(Kinetic.MOD_ID, "telemetry");
    private static final List<String> PASSIVE_RENDERER_MODS = CompatibilityGuard.loadedRendererMods();
    private static final ClientTelemetry TELEMETRY = new ClientTelemetry();
    private static final TelemetryOverlay OVERLAY = new TelemetryOverlay(TELEMETRY, false, KineticConfig.defaultFrameBudget());
    private static final Optional<BenchmarkDriver> BENCHMARK = BenchmarkDriver.requested(
        TELEMETRY.gcPauses(),
        TELEMETRY.jobs(),
        TELEMETRY.mainThread(),
        VulkanMemoryProbe::sample
    );
    private static final Optional<JoinCheck> JOIN_CHECK = JoinCheck.requested();

    private static long frameStart;
    private static long clientTickStart;
    private static double frameSimulatedMillis = MotionSampler.NOT_SAMPLED;
    private static boolean startupReported;
    private static volatile TelemetrySnapshot latestSnapshot;

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
        modBus.addListener(KineticKeys::register);
        modBus.addListener((RegisterDebugEntriesEvent event) -> KineticDebugEntry.register(event, PASSIVE_RENDERER_MODS));
        modBus.addListener(ClientEvents::onRegisterGuiLayers);
        modBus.addListener(ClientEvents::onModelsReloaded);
        ZoomController.register();
        NeoForge.EVENT_BUS.addListener(ClientEvents::onFrameStart);
        NeoForge.EVENT_BUS.addListener(ClientEvents::onFrameEnd);
        NeoForge.EVENT_BUS.addListener(ClientEvents::onFlipFrame);
        NeoForge.EVENT_BUS.addListener(ClientEvents::onClientTickStart);
        NeoForge.EVENT_BUS.addListener(ClientEvents::onClientTickEnd);
        NeoForge.EVENT_BUS.addListener(ClientEvents::onLoggingIn);
        NeoForge.EVENT_BUS.addListener(ClientEvents::onLoggingOut);
        NeoForge.EVENT_BUS.addListener(ClientEvents::onShutdown);
        NeoForge.EVENT_BUS.addListener(ClientEvents::onChunkLoaded);
        NeoForge.EVENT_BUS.addListener(ClientEvents::onChunkUnloaded);
        NeoForge.EVENT_BUS.addListener(DistantTerrainFog::apply);
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        if (!PASSIVE_RENDERER_MODS.isEmpty()) {
            Kinetic.LOGGER.warn("Kinetic keeps its optimizations off because these renderer mods are installed: {}", PASSIVE_RENDERER_MODS);
        }
        if (BENCHMARK.isPresent()) {
            Kinetic.LOGGER.info("Kinetic benchmark requested; it starts when the title screen is shown");
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
        OVERLAY.setVisible(BENCHMARK.isPresent());
        OVERLAY.setBudget(KineticConfig.frameBudget());
        TerrainOcclusion.setEnabled(KineticConfig.OCCLUSION_CULLING.getAsBoolean());
        LeafCulling.setEnabled(KineticConfig.LEAF_CULLING.getAsBoolean());
        DistantTerrain.setDistanceChunks(KineticConfig.DISTANT_TERRAIN_DISTANCE.getAsInt());
        DistantTerrain.setQuality(KineticConfig.DISTANT_TERRAIN_QUALITY.get());
        DistantTerrain.setPower(KineticConfig.DISTANT_TERRAIN_POWER.get());
        DistantTerrain.setEnabled(KineticConfig.DISTANT_TERRAIN.getAsBoolean() && PASSIVE_RENDERER_MODS.isEmpty()
            && MixinModules.activeModules().getOrDefault(OptimizationModule.DISTANT_TERRAIN.key(), false));
        SpawnPregenerationSettings.apply(KineticConfig.PREGENERATE.getAsBoolean() && BENCHMARK.isEmpty(), KineticConfig.PREGENERATION_RADIUS.getAsInt());
        if (PASSIVE_RENDERER_MODS.isEmpty()) {
            FrameRateGovernor.apply(KineticConfig.frameRatePolicy());
        }
    }

    private static void onFrameStart(RenderFrameEvent.Pre event) {
        long now = System.nanoTime();
        frameStart = now;
        FrameTrace.frameStarted();
        FrameCostRecorder.SHARED.frameStarted(now);
        GpuStageTimer.frameStarted();
        FrameTrace.renderStarted();
        TELEMETRY.frameStarted(now);
        DistantTerrainRenderer.prepareFrame(Minecraft.getInstance());
        frameSimulatedMillis = MotionSampler.simulatedFrameMillis(Minecraft.getInstance(), event.getPartialTick());
        if (BENCHMARK.isPresent()) {
            BENCHMARK.get().frameStarted(now);
        }
        if (JOIN_CHECK.isPresent()) {
            JOIN_CHECK.get().frameStarted(now);
        }
    }

    private static void sampleMotion(long presentedAt) {
        double simulatedMillis = frameSimulatedMillis;
        if (Double.isNaN(simulatedMillis)) {
            TELEMETRY.motionInterrupted();
            BENCHMARK.ifPresent(BenchmarkDriver::motionInterrupted);
            return;
        }
        TELEMETRY.motionSampled(presentedAt, simulatedMillis);
        if (BENCHMARK.isPresent()) {
            BENCHMARK.get().motionSampled(presentedAt, simulatedMillis);
        }
    }

    private static void onFrameEnd(RenderFrameEvent.Post event) {
        FrameTrace.renderFinished();
        SystemTimings.SHARED.end(TimedSystem.FRAME, frameStart);
        TELEMETRY.frameFinished();
        TerrainOcclusion.frameFinished();
    }

    private static void onFlipFrame(FlipFrameEvent event) {
        FrameTrace.presented();
        sampleMotion(System.nanoTime());
        DirectDraws.endFrame();
        DistantTerrainRenderer.endFrame();
    }

    private static void onClientTickStart(ClientTickEvent.Pre event) {
        FrameTrace.tickStarted();
        clientTickStart = SystemTimings.begin();
    }

    private static void onClientTickEnd(ClientTickEvent.Post event) {
        DistantTerrain.clientTick(Minecraft.getInstance());
        FrameTrace.tickFinished();
        SystemTimings.SHARED.end(TimedSystem.CLIENT_TICK, clientTickStart);
        Minecraft minecraft = Minecraft.getInstance();
        if (!startupReported && minecraft.isGameLoadFinished()) {
            startupReported = true;
            ShaderCaches.reportStartup();
            reportGarbageCollector();
            HardwareSetup.onGameLoaded(minecraft, BENCHMARK.isPresent());
            warmUpForJoining();
        }
        TELEMETRY.clientTick(OVERLAY.budget(), snapshot -> {
            latestSnapshot = snapshot;
            if (OVERLAY.isVisible()) {
                OVERLAY.publish(snapshot, PASSIVE_RENDERER_MODS);
            }
        });
        BENCHMARK.ifPresent(benchmark -> benchmark.clientTick(minecraft));
        JOIN_CHECK.ifPresent(check -> check.clientTick(minecraft));
    }

    static TelemetrySnapshot latestSnapshot() {
        return latestSnapshot;
    }

    private static void warmUpForJoining() {
        if (MixinModules.activeModules().getOrDefault(OptimizationModule.JOIN_WARMUP.key(), false)) {
            JoinWarmup.start();
        }
        if (PASSIVE_RENDERER_MODS.isEmpty()) {
            GpuCulling.warmUp(TELEMETRY.jobs().service());
        }
    }

    private static void reportGarbageCollector() {
        Kinetic.LOGGER.info("Kinetic: {} garbage collector, heap limit {} MB",
            GarbageCollector.current().displayName(), Runtime.getRuntime().maxMemory() / (1024 * 1024));
        GcAdvice.forThisJvm().ifPresent(advice -> Kinetic.LOGGER.warn("Kinetic: for smoother frames, {}", advice));
    }

    private static void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        TELEMETRY.resetFrames();
    }

    private static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        JoinPregeneration.reset();
        DistantTerrain.reset();
    }

    private static void onChunkLoaded(ChunkEvent.Load event) {
        if (event.getLevel().isClientSide()) {
            DistantTerrain.chunkLoaded(event.getChunk());
        }
    }

    private static void onShutdown(GameShuttingDownEvent event) {
        DistantTerrain.shutdown();
        ShaderCaches.shutdown();
        TELEMETRY.close();
    }

    private static void onModelsReloaded(ModelEvent.BakingCompleted event) {
        Minecraft.getInstance().execute(DistantTerrain::resourcesReloaded);
    }

    private static void onChunkUnloaded(ChunkEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            var pos = event.getChunk().getPos();
            DistantTerrain.chunkUnloaded(pos.x(), pos.z());
        }
    }
}
