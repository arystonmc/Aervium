package com.aryston.aervium.client.pregeneration;

import com.aryston.aervium.Aervium;
import com.aryston.aervium.core.pregeneration.JoinProgress;
import com.aryston.aervium.core.pregeneration.PregenerationHold;
import com.aryston.aervium.core.pregeneration.TerrainProgress;
import com.aryston.aervium.core.telemetry.Durations;
import com.aryston.aervium.world.spawn.OwnerTerrainWait;
import com.aryston.aervium.world.spawn.SpawnPregenerationSettings;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.concurrent.TimeUnit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.SectionUpdateTracker;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.LevelLoadTracker;
import net.minecraft.client.renderer.Octree;
import net.minecraft.client.renderer.SectionOcclusionGraph;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.CompiledSectionMesh;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.world.level.ChunkPos;
import org.jspecify.annotations.Nullable;

public final class JoinPregeneration {
    public static final long PACKET_BUDGET_NANOS = TimeUnit.MILLISECONDS.toNanos(10L);
    public static final long LIGHT_BUDGET_NANOS = TimeUnit.MILLISECONDS.toNanos(6L);

    private static final long LIMIT_NANOS = TimeUnit.SECONDS.toNanos(90L);
    private static final long STALL_NANOS = TimeUnit.SECONDS.toNanos(15L);
    private static final long SCAN_BUDGET_NANOS = TimeUnit.MICROSECONDS.toNanos(1500L);
    private static final long GRAPH_INTERVAL_NANOS = TimeUnit.MILLISECONDS.toNanos(100L);
    private static final double FADE_SECONDS_TO_NANOS = TimeUnit.SECONDS.toNanos(1L);
    private static final int BUILDS_PER_FRAME = 64;
    private static final int CHECKS_PER_FRAME = 4096;
    private static final int NO_CLOSE_DISTANCE = 0;

    private static final TerrainReadiness READINESS = new TerrainReadiness();
    private static final OmniFrustum EVERY_DIRECTION = new OmniFrustum();
    private static final ObjectArrayList<SectionRenderDispatcher.RenderSection> GRAPH_SECTIONS = new ObjectArrayList<>();
    private static final ObjectArrayList<SectionRenderDispatcher.RenderSection> BATCH = new ObjectArrayList<>();
    private static final ObjectArrayList<SectionRenderDispatcher.RenderSection> REBUILDS = new ObjectArrayList<>();
    private static final Octree.OctreeVisitor COLLECT_SECTION = (node, fullyVisible, depth, isClose) -> {
        SectionRenderDispatcher.RenderSection section = node.getSection();
        if (section != null) {
            GRAPH_SECTIONS.add(section);
        }
    };
    private static @Nullable LevelLoadTracker tracker;
    private static @Nullable PregenerationHold hold;
    private static TerrainProgress progress = TerrainProgress.NONE;
    private static long lastGraphCopy;
    private static int nextCheck;

    private JoinPregeneration() {
    }

    public static void adopt(LevelLoadTracker loadTracker) {
        reset();
        if (SpawnPregenerationSettings.enabled()) {
            tracker = loadTracker;
            SpawnPregenerationSettings.useRenderDistance(Minecraft.getInstance().options.renderDistance().get());
        }
    }

    public static void clientLoadStarted(LevelLoadTracker loadTracker) {
        if (loadTracker != tracker || hold != null) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        long settle = Math.round(minecraft.options.chunkSectionFadeInTime().get() * FADE_SECONDS_TO_NANOS);
        hold = new PregenerationHold(System.nanoTime(), LIMIT_NANOS, STALL_NANOS, settle);
        progress = TerrainProgress.NONE;
        lastGraphCopy = 0L;
        nextCheck = 0;
        OwnerTerrainWait.set(true);
    }

    public static boolean holds(LevelLoadTracker loadTracker) {
        if (loadTracker != tracker || hold == null) {
            return false;
        }
        long now = System.nanoTime();
        PregenerationHold.State state = hold.update(progress, now);
        if (state == PregenerationHold.State.WAITING) {
            return true;
        }
        finish(state, now);
        return false;
    }

    public static float progress(LevelLoadTracker loadTracker, float serverProgress) {
        if (loadTracker != tracker) {
            return serverProgress;
        }
        return JoinProgress.blend(serverProgress, hold == null ? 0.0F : progress.fraction());
    }

    public static boolean spreadsWork() {
        return tracker != null && Minecraft.getInstance().gui.screen() instanceof LevelLoadingScreen;
    }

    public static ObjectArrayList<SectionRenderDispatcher.RenderSection> sectionsToCompile(ObjectArrayList<SectionRenderDispatcher.RenderSection> visible,
        @Nullable SectionUpdateTracker dirty) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        ViewArea viewArea = minecraft.levelRenderer.viewArea();
        if (hold == null || dirty == null || level == null || minecraft.player == null || viewArea == null) {
            return visible;
        }
        long now = System.nanoTime();
        SectionOcclusionGraph graph = minecraft.levelRenderer.sectionOcclusionGraph();
        scan(level, viewArea, graph, minecraft.player.chunkPosition(), minecraft.options.getEffectiveRenderDistance(), now);
        if (now - lastGraphCopy >= GRAPH_INTERVAL_NANOS) {
            lastGraphCopy = now;
            GRAPH_SECTIONS.clear();
            graph.getOctree().visitNodes(COLLECT_SECTION, EVERY_DIRECTION, NO_CLOSE_DISTANCE);
        }
        return nextBatch(level, dirty);
    }

    public static void reset() {
        tracker = null;
        hold = null;
        progress = TerrainProgress.NONE;
        GRAPH_SECTIONS.clear();
        BATCH.clear();
        REBUILDS.clear();
        OwnerTerrainWait.set(false);
    }

    private static void scan(ClientLevel level, ViewArea viewArea, SectionOcclusionGraph graph, ChunkPos center, int renderDistance, long now) {
        int radius = Math.min(SpawnPregenerationSettings.joinRadius(), renderDistance);
        if (!READINESS.covers(center, radius)) {
            READINESS.reset(center, radius);
        }
        READINESS.advance(level, viewArea, graph, now + SCAN_BUDGET_NANOS);
        progress = READINESS.progress(((PendingLightUpdates) level).aervium$pendingLightUpdates());
    }

    private static ObjectArrayList<SectionRenderDispatcher.RenderSection> nextBatch(ClientLevel level, SectionUpdateTracker dirty) {
        BATCH.clear();
        REBUILDS.clear();
        int size = GRAPH_SECTIONS.size();
        for (int checked = 0; checked < size && checked < CHECKS_PER_FRAME && BATCH.size() < BUILDS_PER_FRAME; checked++) {
            if (nextCheck >= size) {
                nextCheck = 0;
            }
            SectionRenderDispatcher.RenderSection section = GRAPH_SECTIONS.get(nextCheck++);
            long node = section.getSectionNode();
            SectionUpdateTracker.SectionDirtyState state = dirty.getDirtyState(node);
            if (state == null || !state.isDirty()) {
                continue;
            }
            if (section.getSectionMesh() != CompiledSectionMesh.UNCOMPILED) {
                if (REBUILDS.size() < BUILDS_PER_FRAME) {
                    REBUILDS.add(section);
                }
            } else if (dirty.hasAllNeighbors(level, node)) {
                BATCH.add(section);
            }
        }
        for (int index = 0; index < REBUILDS.size() && BATCH.size() < BUILDS_PER_FRAME; index++) {
            BATCH.add(REBUILDS.get(index));
        }
        return BATCH;
    }

    private static void finish(PregenerationHold.State state, long now) {
        long waited = Math.round(Durations.millis(hold.elapsedNanos(now)));
        switch (state) {
            case READY -> Aervium.LOGGER.info("Aervium pregeneration: {} chunks and {} sections ready around the player after {} ms",
                progress.chunksReady(), progress.sectionsReady(), waited);
            case TIMED_OUT, STALLED -> Aervium.LOGGER.warn("Aervium pregeneration {} after {} ms with {} of {} chunks and {} of {} sections ready; entering anyway",
                state == PregenerationHold.State.STALLED ? "stalled" : "timed out", waited,
                progress.chunksReady(), progress.chunksRequired(), progress.sectionsReady(), progress.sectionsRequired());
            case WAITING -> {
            }
        }
        reset();
    }
}
