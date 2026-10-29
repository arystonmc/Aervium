package com.aryston.kinetic.client.benchmark;

import com.aryston.kinetic.Kinetic;
import com.aryston.kinetic.debug.benchmark.BenchmarkAnchors;
import com.aryston.kinetic.debug.benchmark.BenchmarkCommands;
import com.aryston.kinetic.debug.benchmark.BenchmarkScene;
import com.aryston.kinetic.debug.benchmark.BlockPoint;
import com.aryston.kinetic.debug.benchmark.BoxCommands;
import com.aryston.kinetic.debug.benchmark.SceneSetup;
import com.aryston.kinetic.debug.benchmark.SceneSite;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

final class BenchmarkWorld {
    static final long SEED = 8_675_309L;

    private static final String FOLDER = "kinetic-benchmark";
    private static final String LEVEL_NAME = "Kinetic Benchmark";
    private static final int CHUNK_MASK = 15;

    private BenchmarkWorld() {
    }

    static void createFresh(Minecraft minecraft, Screen parent) throws IOException {
        LevelSettings settings = new LevelSettings(
            LEVEL_NAME,
            GameType.SPECTATOR,
            new LevelSettings.DifficultySettings(Difficulty.PEACEFUL, false, false),
            true,
            WorldDataConfiguration.DEFAULT
        );
        FreshWorld.create(minecraft, parent, FOLDER, settings, SEED);
    }

    static CompletableFuture<BenchmarkAnchors> prepare(IntegratedServer server) {
        return CompletableFuture.supplyAsync(() -> {
            run(server, BenchmarkCommands.worldRules());
            return locateAnchors(server.overworld());
        }, server);
    }

    static void loadSite(IntegratedServer server, BenchmarkScene scene, BlockPoint site) {
        SceneSetup setup = scene.setup();
        if (setup.usesSite()) {
            server.execute(() -> run(server, List.of(BoxCommands.forceLoad(site, setup.far(site), true))));
        }
    }

    static CompletableFuture<Boolean> buildWhenLoaded(IntegratedServer server, BenchmarkScene scene, BlockPoint site) {
        return server.submit(() -> {
            SceneSetup setup = scene.setup();
            if (!loaded(server.overworld(), site, setup.far(site))) {
                return false;
            }
            Kinetic.LOGGER.info("Kinetic benchmark: building the {} scene at {}", scene.id(), site);
            run(server, setup.build(site));
            return true;
        });
    }

    static void tickScene(IntegratedServer server, BenchmarkScene scene, BlockPoint site, int tick) {
        List<String> commands = scene.setup().everyTick(site, tick);
        if (!commands.isEmpty()) {
            server.execute(() -> run(server, commands));
        }
    }

    static void clearSite(IntegratedServer server, BenchmarkScene scene, BlockPoint site) {
        SceneSetup setup = scene.setup();
        if (!setup.usesSite()) {
            return;
        }
        server.execute(() -> {
            run(server, setup.clear(site));
            run(server, BoxCommands.killLooseDrops());
            run(server, List.of(BoxCommands.forceLoad(site, setup.far(site), false)));
        });
    }

    private static boolean loaded(ServerLevel level, BlockPoint from, BlockPoint to) {
        for (int chunkX = SectionPos.blockToSectionCoord(from.x()); chunkX <= SectionPos.blockToSectionCoord(to.x()); chunkX++) {
            for (int chunkZ = SectionPos.blockToSectionCoord(from.z()); chunkZ <= SectionPos.blockToSectionCoord(to.z()); chunkZ++) {
                if (!level.areEntitiesLoaded(ChunkPos.pack(chunkX, chunkZ))) {
                    return false;
                }
            }
        }
        return true;
    }

    private static BenchmarkAnchors locateAnchors(ServerLevel level) {
        BlockPos spawnPosition = level.getRespawnData().pos();
        BlockPoint spawn = surface(level, spawnPosition.getX(), spawnPosition.getZ());
        Map<String, BlockPoint> sites = new LinkedHashMap<>();
        for (BenchmarkScene scene : BenchmarkScene.values()) {
            SceneSite site = scene.site();
            if (scene.setup().usesSite()) {
                sites.put(scene.id(), surface(level, spawn.x() + site.eastOffset(), spawn.z() + site.southOffset()).offset(0, site.height(), 0));
            }
        }
        return new BenchmarkAnchors(spawn, sites);
    }

    private static BlockPoint surface(ServerLevel level, int x, int z) {
        return new BlockPoint(x, surfaceY(level, x, z), z);
    }

    private static int surfaceY(ServerLevel level, int x, int z) {
        LevelChunk chunk = level.getChunk(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z));
        return chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x & CHUNK_MASK, z & CHUNK_MASK) + 1;
    }

    private static void run(IntegratedServer server, List<String> commands) {
        CommandSourceStack source = server.createCommandSourceStack().withSource(CommandFailureLog.INSTANCE);
        commands.forEach(command -> server.getCommands().performPrefixedCommand(source, command));
    }
}
