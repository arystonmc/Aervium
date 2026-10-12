package com.aryston.aervium.world.parity;

import com.aryston.aervium.Aervium;
import com.aryston.aervium.core.parity.StateHash;
import com.aryston.aervium.mixin.LevelRandomAccessor;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.LockSupport;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ThreadedLevelLightEngine;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class ParityHarness {
    private static final String PROPERTY = "aervium.parity";
    private static final String TICKS_PROPERTY = "aervium.parity.ticks";
    private static final String LIGHT_CHANGES_PROPERTY = "aervium.parity.lightChanges";
    private static final int DEFAULT_TICKS = 1200;
    private static final int DEFAULT_LIGHT_CHANGES = 4;
    private static final int SETTLE_TICKS = 40;
    private static final int CHECKPOINT_TICKS = 200;
    private static final long SEED = 0x41657276L;
    private static final int RAND_VALUE_SEED = 0x1F2E3D4C;

    private static int tick;
    private static boolean running;

    private ParityHarness() {
    }

    public static void register() {
        if (!Boolean.getBoolean(PROPERTY)) {
            return;
        }
        NeoForge.EVENT_BUS.addListener(ParityHarness::onServerStarted);
        NeoForge.EVENT_BUS.addListener(ParityHarness::onServerTickEnd);
    }

    private static void onServerStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        ServerLevel level = server.overworld();
        GameRules rules = level.getGameRules();
        rules.set(GameRules.ADVANCE_TIME, false, server);
        rules.set(GameRules.ADVANCE_WEATHER, false, server);
        rules.set(GameRules.SPAWN_MOBS, false, server);
        forEachChunk((chunkX, chunkZ) -> level.setChunkForced(chunkX, chunkZ, true));
        tick = 0;
        running = true;
        Aervium.LOGGER.info("Aervium parity: world ready, settling for {} ticks", SETTLE_TICKS);
    }

    private static void onServerTickEnd(ServerTickEvent.Post event) {
        if (!running) {
            return;
        }
        MinecraftServer server = event.getServer();
        ServerLevel level = server.overworld();
        tick++;
        if (tick == SETTLE_TICKS) {
            level.getRandom().setSeed(SEED);
            ((LevelRandomAccessor) level).aervium$setRandValue(RAND_VALUE_SEED);
            ParityScenario.build(level);
            Aervium.LOGGER.info("Aervium parity: scenario built, running {} ticks", ticks());
            return;
        }
        int scenarioTick = tick - SETTLE_TICKS;
        if (scenarioTick > 0 && scenarioTick < ticks()) {
            ParityScenario.changeLight(level, scenarioTick, Integer.getInteger(LIGHT_CHANGES_PROPERTY, DEFAULT_LIGHT_CHANGES));
        }
        if (scenarioTick <= 0 || scenarioTick % CHECKPOINT_TICKS != 0 && scenarioTick != ticks()) {
            return;
        }
        finishLightUpdates(level);
        Aervium.LOGGER.info("Aervium parity: tick {} blocks {} light {} entities {}",
            scenarioTick, blockHash(level), lightHash(level), entityHash(level));
        if (scenarioTick >= ticks()) {
            running = false;
            Aervium.LOGGER.info("Aervium parity: finished");
            server.halt(false);
        }
    }

    /**
     * The server lights the world on its own worker, so a hash taken right after a block change could see the
     * light half done. Waits until every light task queued for the scenario chunks has run.
     */
    private static void finishLightUpdates(ServerLevel level) {
        ThreadedLevelLightEngine lightEngine = level.getChunkSource().getLightEngine();
        List<CompletableFuture<?>> pending = new ArrayList<>();
        forEachChunk((chunkX, chunkZ) -> pending.add(lightEngine.waitForPendingTasks(chunkX, chunkZ)));
        CompletableFuture<Void> all = CompletableFuture.allOf(pending.toArray(CompletableFuture[]::new));
        while (!all.isDone()) {
            lightEngine.tryScheduleUpdate();
            LockSupport.parkNanos(100_000L);
        }
    }

    private static void forEachChunk(ChunkAction action) {
        for (int chunkX = SectionPos.blockToSectionCoord(ParityScenario.MIN_X); chunkX <= SectionPos.blockToSectionCoord(ParityScenario.MAX_X); chunkX++) {
            for (int chunkZ = SectionPos.blockToSectionCoord(ParityScenario.MIN_Z); chunkZ <= SectionPos.blockToSectionCoord(ParityScenario.MAX_Z); chunkZ++) {
                action.accept(chunkX, chunkZ);
            }
        }
    }

    @FunctionalInterface
    private interface ChunkAction {
        void accept(int chunkX, int chunkZ);
    }

    private static int ticks() {
        return Integer.getInteger(TICKS_PROPERTY, DEFAULT_TICKS);
    }

    private static String blockHash(ServerLevel level) {
        StateHash hash = new StateHash();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        forEachPosition(pos, () -> {
            hash.add(Block.getId(level.getBlockState(pos)));
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity != null) {
                hash.add(blockEntity.saveWithoutMetadata(level.registryAccess()).toString());
            }
            hash.add(level.getBlockTicks().hasScheduledTick(pos, level.getBlockState(pos).getBlock()) ? 1 : 0);
        });
        return hash.hex();
    }

    private static String lightHash(ServerLevel level) {
        StateHash hash = new StateHash();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        forEachPosition(pos, () -> {
            hash.add(level.getBrightness(LightLayer.BLOCK, pos));
            hash.add(level.getBrightness(LightLayer.SKY, pos));
        });
        return hash.hex();
    }

    private static String entityHash(ServerLevel level) {
        StateHash hash = new StateHash();
        AABB area = new AABB(ParityScenario.MIN_X, ParityScenario.MIN_Y, ParityScenario.MIN_Z,
            ParityScenario.MAX_X + 1, ParityScenario.MAX_Y + 1, ParityScenario.MAX_Z + 1);
        for (Entity entity : level.getEntities((Entity) null, area, entity -> true)) {
            hash.add(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString());
            hash.add(entity.blockPosition().asLong());
        }
        return hash.hex();
    }

    private static void forEachPosition(BlockPos.MutableBlockPos pos, Runnable action) {
        for (int y = ParityScenario.MIN_Y; y <= ParityScenario.MAX_Y; y++) {
            for (int z = ParityScenario.MIN_Z; z <= ParityScenario.MAX_Z; z++) {
                for (int x = ParityScenario.MIN_X; x <= ParityScenario.MAX_X; x++) {
                    pos.set(x, y, z);
                    action.run();
                }
            }
        }
    }
}
