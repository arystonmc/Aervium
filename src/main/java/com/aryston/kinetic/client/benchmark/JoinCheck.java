package com.aryston.kinetic.client.benchmark;

import com.aryston.kinetic.Kinetic;
import com.aryston.kinetic.core.telemetry.Durations;
import com.aryston.kinetic.core.telemetry.FrameTimeRecorder;
import com.aryston.kinetic.core.telemetry.FrameTimeSummary;
import com.aryston.kinetic.debug.benchmark.JoinTimeline;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.chunk.CompiledSectionMesh;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.SectionPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;

public final class JoinCheck {
    private static final String PROPERTY = "kinetic.joinCheck";
    private static final String QUIT_PROPERTY = "kinetic.joinCheck.quit";
    private static final String NEW_WORLD_PROPERTY = "kinetic.joinCheck.newWorld";
    private static final String WORLD_PROPERTY = "kinetic.joinCheck.world";
    private static final String DELAY_PROPERTY = "kinetic.joinCheck.delay";
    private static final int DEFAULT_DELAY_SECONDS = 5;
    private static final long NOT_SHOWN = -1L;
    private static final String WORLD_FOLDER = "kinetic-join-check";
    private static final String WORLD_NAME = "Kinetic Join Check";
    private static final long SEED = 8_675_309L;
    private static final String GAME = "game";
    private static final String TERRAIN = "terrain";
    private static final String SERVER = "server";
    private static final Set<String> LOADING_PHASES = Set.of("reading world data", "loading data packs", "progress", SERVER, "connecting");
    private static final long AFTER_ENTRY_NANOS = TimeUnit.SECONDS.toNanos(10L);
    private static final long SAMPLE_INTERVAL_NANOS = TimeUnit.SECONDS.toNanos(1L);
    private static final int FRAME_CAPACITY = 1 << 15;

    private final boolean quitWhenDone = Boolean.getBoolean(QUIT_PROPERTY);
    private final boolean newWorld = Boolean.getBoolean(NEW_WORLD_PROPERTY);
    private final String world = System.getProperty(WORLD_PROPERTY, "");
    private final long delayNanos = TimeUnit.SECONDS.toNanos(Integer.getInteger(DELAY_PROPERTY, DEFAULT_DELAY_SECONDS));
    private long titleShownAt = NOT_SHOWN;
    private final JoinTimeline timeline = new JoinTimeline();
    private final FrameTimeRecorder afterEntry = new FrameTimeRecorder(FRAME_CAPACITY);
    private final JoinShots shots = new JoinShots();
    private final List<String> samples = new ArrayList<>();
    private Stage stage = Stage.WAITING;
    private boolean worldRequested;
    private long enteredAt;
    private long nextSample;

    private JoinCheck() {
    }

    public static Optional<JoinCheck> requested() {
        return Boolean.getBoolean(PROPERTY) ? Optional.of(new JoinCheck()) : Optional.empty();
    }

    public void frameStarted(long now) {
        Minecraft minecraft = Minecraft.getInstance();
        String phase = phase(minecraft);
        switch (stage) {
            case WAITING -> {
                if (LOADING_PHASES.contains(phase)) {
                    stage = Stage.LOADING;
                    timeline.frame(now, phase);
                }
            }
            case LOADING -> {
                timeline.frame(now, phase);
                if (GAME.equals(phase)) {
                    enter(minecraft, now);
                    shots.entered(now);
                } else {
                    shots.loading(minecraft, now, timeline.elapsedNanos(now), phase);
                }
            }
            case IN_GAME -> {
                afterEntry.frameStarted(now);
                shots.inGame(minecraft, now, now - enteredAt);
            }
            case DONE -> {
            }
        }
    }

    public void clientTick(Minecraft minecraft) {
        if (stage == Stage.WAITING && !worldRequested && (newWorld || !world.isEmpty()) && atTitle(minecraft)) {
            openWorld(minecraft);
        }
        if (stage != Stage.IN_GAME) {
            return;
        }
        long now = System.nanoTime();
        if (now >= nextSample) {
            nextSample += SAMPLE_INTERVAL_NANOS;
            samples.add(terrainState(minecraft));
        }
        if (now - enteredAt >= AFTER_ENTRY_NANOS) {
            finish(minecraft);
        }
    }

    private boolean atTitle(Minecraft minecraft) {
        if (!minecraft.isGameLoadFinished() || minecraft.level != null || minecraft.gui.screen() == null) {
            return false;
        }
        long now = System.nanoTime();
        if (titleShownAt == NOT_SHOWN) {
            titleShownAt = now;
        }
        return now - titleShownAt >= delayNanos;
    }

    private void openWorld(Minecraft minecraft) {
        worldRequested = true;
        Screen title = minecraft.gui.screen();
        if (newWorld) {
            createWorld(minecraft, title);
        } else {
            Kinetic.LOGGER.info("Kinetic join check: opening {}", world);
            minecraft.createWorldOpenFlows().openWorld(world, () -> minecraft.gui.setScreen(title));
        }
    }

    private void createWorld(Minecraft minecraft, Screen parent) {
        try {
            LevelSettings settings = new LevelSettings(WORLD_NAME, GameType.SURVIVAL, new LevelSettings.DifficultySettings(Difficulty.NORMAL, false, false),
                true, WorldDataConfiguration.DEFAULT);
            FreshWorld.create(minecraft, parent, WORLD_FOLDER, settings, SEED);
            Kinetic.LOGGER.info("Kinetic join check: creating a fresh world with seed {}", SEED);
        } catch (IOException failure) {
            Kinetic.LOGGER.error("Kinetic join check: the world could not be created", failure);
            stage = Stage.DONE;
        }
    }

    private void enter(Minecraft minecraft, long now) {
        stage = Stage.IN_GAME;
        enteredAt = now;
        nextSample = now + SAMPLE_INTERVAL_NANOS;
        Kinetic.LOGGER.info("Kinetic join check: entered the world after {} ms at render distance {}; at entry {}",
            Math.round(Durations.millis(timeline.elapsedNanos(now))), minecraft.options.getEffectiveRenderDistance(), terrainState(minecraft));
    }

    private void finish(Minecraft minecraft) {
        stage = Stage.DONE;
        timeline.lines().forEach(line -> Kinetic.LOGGER.info("Kinetic join check: {}", line));
        long[] frames = afterEntry.frameTimes().latest(FRAME_CAPACITY);
        FrameTimeSummary summary = FrameTimeSummary.of(frames, frames.length);
        Kinetic.LOGGER.info(String.format(Locale.ROOT,
            "Kinetic join check: first 10 s in game: %.1f FPS average, 1%% low %.1f, 0.1%% low %.1f, longest %.1f ms, %d frames over 50 ms",
            summary.averageFps(), summary.onePercentLowFps(), summary.pointOnePercentLowFps(), summary.maxMs(), summary.framesOver50Ms()));
        Kinetic.LOGGER.info("Kinetic join check: each second after entry: {}", String.join(" | ", samples));
        if (quitWhenDone) {
            minecraft.stop();
        }
    }

    private static String terrainState(Minecraft minecraft) {
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null) {
            return "no world";
        }
        return String.format(Locale.ROOT, "%d chunks missing, %d visible sections not built", missingChunks(minecraft, level), pendingSections(minecraft, level));
    }

    private static int missingChunks(Minecraft minecraft, ClientLevel level) {
        ChunkPos center = minecraft.player.chunkPosition();
        int radius = minecraft.options.getEffectiveRenderDistance();
        int missing = 0;
        for (int chunkX = center.x() - radius; chunkX <= center.x() + radius; chunkX++) {
            for (int chunkZ = center.z() - radius; chunkZ <= center.z() + radius; chunkZ++) {
                if (ChunkTrackingView.isInViewDistance(center.x(), center.z(), radius, chunkX, chunkZ) && !level.getChunkSource().hasChunk(chunkX, chunkZ)) {
                    missing++;
                }
            }
        }
        return missing;
    }

    private static int pendingSections(Minecraft minecraft, ClientLevel level) {
        int pending = 0;
        for (SectionRenderDispatcher.RenderSection section : minecraft.levelRenderer.visibleSections()) {
            if (section.getSectionMesh() != CompiledSectionMesh.UNCOMPILED) {
                continue;
            }
            long node = section.getSectionNode();
            LevelChunk chunk = level.getChunkSource().getChunk(SectionPos.x(node), SectionPos.z(node), ChunkStatus.FULL, false);
            int index = level.getSectionIndexFromSectionY(SectionPos.y(node));
            if (chunk == null || index >= 0 && index < chunk.getSectionsCount() && !chunk.getSection(index).hasOnlyAir()) {
                pending++;
            }
        }
        return pending;
    }

    private static String phase(Minecraft minecraft) {
        Screen screen = minecraft.gui.screen();
        if (screen == null) {
            return minecraft.level != null ? GAME : "none";
        }
        if (screen instanceof LevelLoadingScreen) {
            return minecraft.level == null ? SERVER : TERRAIN;
        }
        if (screen instanceof GenericMessageScreen) {
            return messagePhase(screen.getTitle());
        }
        if (screen instanceof ProgressScreen) {
            return "progress";
        }
        if (screen instanceof ConnectScreen) {
            return "connecting";
        }
        return screen.getClass().getSimpleName();
    }

    private static String messagePhase(Component title) {
        String key = title.getContents() instanceof TranslatableContents contents ? contents.getKey() : "";
        return switch (key) {
            case "selectWorld.data_read" -> "reading world data";
            case "selectWorld.resource_load" -> "loading data packs";
            default -> "message " + key;
        };
    }

    private enum Stage {
        WAITING,
        LOADING,
        IN_GAME,
        DONE
    }
}
