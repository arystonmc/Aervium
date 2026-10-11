package com.aryston.aervium.client.benchmark;

import com.aryston.aervium.Aervium;
import com.aryston.aervium.debug.benchmark.BenchmarkAnchors;
import com.aryston.aervium.debug.benchmark.BenchmarkCommands;
import com.aryston.aervium.debug.benchmark.BenchmarkScene;
import com.aryston.aervium.debug.benchmark.BlockPoint;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.LevelStorageSource;

final class BenchmarkWorld {
    static final long SEED = 8_675_309L;

    private static final String FOLDER = "aervium-benchmark";
    private static final String LEVEL_NAME = "Aervium Benchmark";
    private static final int VILLAGE_SEARCH_RADIUS_CHUNKS = 100;
    private static final int REDSTONE_OFFSET = 96;
    private static final int CHUNK_MASK = 15;

    private BenchmarkWorld() {
    }

    static void createFresh(Minecraft minecraft, Screen parent) throws IOException {
        LevelStorageSource levels = minecraft.getLevelSource();
        if (levels.levelExists(FOLDER)) {
            try (LevelStorageSource.LevelStorageAccess access = levels.createAccess(FOLDER)) {
                access.deleteLevel();
            }
        }
        LevelSettings settings = new LevelSettings(
            LEVEL_NAME,
            GameType.SPECTATOR,
            new LevelSettings.DifficultySettings(Difficulty.PEACEFUL, false, false),
            true,
            WorldDataConfiguration.DEFAULT
        );
        minecraft.createWorldOpenFlows().createFreshLevel(FOLDER, settings, new WorldOptions(SEED, true, false), WorldPresets::createNormalWorldDimensions, parent);
    }

    static CompletableFuture<BenchmarkAnchors> prepare(IntegratedServer server) {
        return CompletableFuture.supplyAsync(() -> {
            run(server, BenchmarkCommands.worldRules());
            return locateAnchors(server.overworld());
        }, server);
    }

    static void setUpScene(IntegratedServer server, BenchmarkScene scene, BenchmarkAnchors anchors) {
        server.execute(() -> {
            ServerLevel level = server.overworld();
            switch (scene) {
                case VILLAGE -> {
                    Aervium.LOGGER.info("Aervium benchmark: summoning {} villagers", BenchmarkCommands.crowdSize());
                    run(server, BenchmarkCommands.villageCrowd(anchors.village(), (x, z) -> surfaceY(level, x, z)));
                }
                case REDSTONE -> {
                    Aervium.LOGGER.info("Aervium benchmark: building {} redstone clocks", BenchmarkCommands.clockCount());
                    run(server, BenchmarkCommands.redstoneBuild(anchors.redstone()));
                }
                default -> {
                }
            }
        });
    }

    private static BenchmarkAnchors locateAnchors(ServerLevel level) {
        BlockPos spawnPosition = level.getRespawnData().pos();
        BlockPoint spawn = surface(level, spawnPosition.getX(), spawnPosition.getZ());
        BlockPos village = level.findNearestMapStructure(StructureTags.VILLAGE, spawnPosition, VILLAGE_SEARCH_RADIUS_CHUNKS, false);
        BlockPoint villageCenter = village == null ? spawn : surface(level, village.getX(), village.getZ());
        BlockPoint redstoneGround = surface(level, spawn.x() + REDSTONE_OFFSET, spawn.z() - REDSTONE_OFFSET);
        BlockPoint redstone = redstoneGround.offset(0, BenchmarkCommands.REDSTONE_PLATFORM_HEIGHT, 0);
        return new BenchmarkAnchors(spawn, villageCenter, village != null, redstone);
    }

    private static BlockPoint surface(ServerLevel level, int x, int z) {
        return new BlockPoint(x, surfaceY(level, x, z), z);
    }

    private static int surfaceY(ServerLevel level, int x, int z) {
        LevelChunk chunk = level.getChunk(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z));
        return chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x & CHUNK_MASK, z & CHUNK_MASK) + 1;
    }

    private static void run(IntegratedServer server, List<String> commands) {
        commands.forEach(command -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command));
    }
}
