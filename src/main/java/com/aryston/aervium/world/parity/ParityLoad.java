package com.aryston.aervium.world.parity;

import com.aryston.aervium.Aervium;
import com.aryston.aervium.world.collision.CollisionAudit;
import com.aryston.aervium.world.hopper.HopperAudit;
import com.aryston.aervium.world.poi.PoiAudit;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.vehicle.minecart.MinecartChest;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

final class ParityLoad {
    private static final List<EntityType<?>> MOB_TYPES = List.of(
        EntityTypes.COW, EntityTypes.SHEEP, EntityTypes.PIG, EntityTypes.CHICKEN, EntityTypes.VILLAGER);
    private static final long SEED = 0x4C6F6164L;
    private static final int MEASURE_FROM_TICK = 100;
    private static final int MAX_ATTEMPTS_PER_MOB = 20;

    private static final ThreadMXBean THREADS = ManagementFactory.getThreadMXBean();

    private static long tickStart;
    private static long tickCpuStart;
    private static long tickCpuNanos;
    private static long tickNanos;
    private static int measuredTicks;
    private static long maxTickNanos;
    private static int mobs;
    private static int hoppers;

    private ParityLoad() {
    }

    static void spawnMobs(ServerLevel level, int count, int forceRadius) {
        mobs = count;
        if (count <= 0) {
            return;
        }
        RandomSource random = RandomSource.create(SEED);
        int span = (forceRadius * 2 + 1) * 16;
        int min = -forceRadius * 16;
        int spawned = 0;
        for (int attempt = 0; spawned < count && attempt < count * MAX_ATTEMPTS_PER_MOB; attempt++) {
            int x = min + random.nextInt(span);
            int z = min + random.nextInt(span);
            BlockPos pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
            if (!level.getFluidState(pos.below()).isEmpty() || !level.getFluidState(pos).isEmpty()) {
                continue;
            }
            if (MOB_TYPES.get(spawned % MOB_TYPES.size()).spawn(level, pos, EntitySpawnReason.COMMAND) != null) {
                spawned++;
            }
        }
        mobs = spawned;
        Aervium.LOGGER.info("Aervium parity: spawned {} mobs on dry land for load", spawned);
    }

    static void placeHoppers(ServerLevel level, int count, int forceRadius) {
        if (count <= 0) {
            return;
        }
        RandomSource random = RandomSource.create(SEED + 1);
        int span = (forceRadius * 2 + 1) * 16;
        int min = -forceRadius * 16;
        int placed = 0;
        for (int attempt = 0; placed < count && attempt < count * MAX_ATTEMPTS_PER_MOB; attempt++) {
            BlockPos pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                new BlockPos(min + random.nextInt(span), 0, min + random.nextInt(span)));
            if (!level.getFluidState(pos.below()).isEmpty() || !level.getBlockState(pos).isAir()) {
                continue;
            }
            level.setBlock(pos, Blocks.HOPPER.defaultBlockState(), Block.UPDATE_ALL);
            if (HopperAudit.ENABLED && placed % 64 == 0) {
                MinecartChest cart = EntityTypes.CHEST_MINECART.spawn(level, pos.above(), EntitySpawnReason.COMMAND);
                if (cart != null) {
                    for (int slot = 0; slot < cart.getContainerSize(); slot++) {
                        cart.setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
                    }
                }
            } else if (HopperAudit.ENABLED && placed % 64 == 32) {
                level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, new ItemStack(Items.COBBLESTONE, 16)));
            }
            placed++;
        }
        hoppers = placed;
        Aervium.LOGGER.info("Aervium parity: placed {} hoppers for load", placed);
    }

    static void tickStarted(ServerTickEvent.Pre event) {
        tickStart = System.nanoTime();
        tickCpuStart = THREADS.getCurrentThreadCpuTime();
    }

    static void tickFinished(int scenarioTick) {
        if (mobs + hoppers <= 0 || scenarioTick < MEASURE_FROM_TICK) {
            return;
        }
        long nanos = System.nanoTime() - tickStart;
        tickCpuNanos += THREADS.getCurrentThreadCpuTime() - tickCpuStart;
        tickNanos += nanos;
        maxTickNanos = Math.max(maxTickNanos, nanos);
        measuredTicks++;
    }

    static void report() {
        if (CollisionAudit.ENABLED) {
            Aervium.LOGGER.info("Aervium collision audit: {}", CollisionAudit.summary());
        }
        if (HopperAudit.ENABLED) {
            Aervium.LOGGER.info("Aervium hopper audit: {}", HopperAudit.summary());
        }
        if (PoiAudit.ENABLED) {
            Aervium.LOGGER.info("Aervium poi audit: {}", PoiAudit.summary());
        }
        if (mobs + hoppers <= 0 || measuredTicks == 0) {
            return;
        }
        Aervium.LOGGER.info("Aervium parity load: {} mobs, {} hoppers, {} ticks, mean tick {} ms ({} ms CPU), longest {} ms", mobs, hoppers, measuredTicks,
            String.format(Locale.ROOT, "%.3f", tickNanos / 1e6 / measuredTicks),
            String.format(Locale.ROOT, "%.3f", tickCpuNanos / 1e6 / measuredTicks),
            String.format(Locale.ROOT, "%.2f", maxTickNanos / 1e6));
    }
}
