package com.aryston.aervium.world.parity;

import com.aryston.aervium.Aervium;
import com.aryston.aervium.core.parity.StateHash;
import com.aryston.aervium.world.light.ServerLightTimings;
import com.sun.management.OperatingSystemMXBean;
import java.lang.management.ManagementFactory;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.Holder;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.PalettedContainerFactory;
import net.minecraft.world.level.chunk.PalettedContainerRO;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Generates a square of chunks on a fixed seed and logs one hash of the terrain (noise, surface and carvers) and
 * biomes of every chunk, plus how long the generation took. The same seed must give the same hash with modules on
 * and off. Features are left out on purpose: vanilla places overlapping features of neighbouring chunks in thread
 * order, so two vanilla runs already differ there (an andesite blob over a diorite blob, for example).
 */
public final class WorldgenCheck {
    private static final String PROPERTY = "aervium.parity.worldgen";
    private static final String RADIUS_PROPERTY = "aervium.parity.worldgenRadius";
    private static final int DEFAULT_RADIUS = 12;
    private static final int SECTION_SIZE = 16;
    private static final int QUARTS_PER_SECTION = 4;
    private static final double NANOS_PER_MILLI = 1.0E6;
    private static final Map<Long, Long> TERRAIN_HASHES = new ConcurrentHashMap<>();

    private static volatile boolean recording;
    private static long startNanos;
    private static long startCpuNanos;
    private static boolean running;

    private WorldgenCheck() {
    }

    public static boolean enabled() {
        return Boolean.getBoolean(PROPERTY);
    }

    public static boolean recording() {
        return recording;
    }

    public static void register() {
        recording = true;
        NeoForge.EVENT_BUS.addListener(WorldgenCheck::onServerStarted);
        NeoForge.EVENT_BUS.addListener(WorldgenCheck::onServerTickEnd);
    }

    /** Called on a worldgen worker when a chunk has its terrain; hashes its blocks and biomes. */
    public static ChunkAccess terrainBuilt(ChunkAccess chunk) {
        StateHash hash = new StateHash();
        for (LevelChunkSection section : chunk.getSections()) {
            for (int y = 0; y < SECTION_SIZE; y++) {
                for (int z = 0; z < SECTION_SIZE; z++) {
                    for (int x = 0; x < SECTION_SIZE; x++) {
                        hash.add(Block.getId(section.getBlockState(x, y, z)));
                    }
                }
            }
            for (int y = 0; y < QUARTS_PER_SECTION; y++) {
                for (int z = 0; z < QUARTS_PER_SECTION; z++) {
                    for (int x = 0; x < QUARTS_PER_SECTION; x++) {
                        Holder<Biome> biome = section.getNoiseBiome(x, y, z);
                        hash.add(biome.unwrapKey().map(key -> key.identifier().toString()).orElse("?"));
                    }
                }
            }
        }
        TERRAIN_HASHES.put(chunk.getPos().pack(), hash.value());
        return chunk;
    }

    /**
     * Saves the blocks and biomes of every section with the real chunk codecs and loads them back; returns the number
     * of sections, how many hold a single block and biome value, and how many came back different.
     */
    private static int[] checkSaves(ServerLevel level, int radius) {
        PalettedContainerFactory factory = level.palettedContainerFactory();
        int sections = 0;
        int singleValue = 0;
        int mismatches = 0;
        for (int chunkX = -radius; chunkX <= radius; chunkX++) {
            for (int chunkZ = -radius; chunkZ <= radius; chunkZ++) {
                for (LevelChunkSection section : level.getChunkSource().getChunkNow(chunkX, chunkZ).getSections()) {
                    sections++;
                    Tag blockTag = factory.blockStatesContainerCodec().encodeStart(NbtOps.INSTANCE, section.getStates()).getOrThrow();
                    Tag biomeTag = factory.biomeContainerCodec().encodeStart(NbtOps.INSTANCE, section.getBiomes()).getOrThrow();
                    PalettedContainer<BlockState> blocks = factory.blockStatesContainerCodec().parse(NbtOps.INSTANCE, blockTag).getOrThrow();
                    PalettedContainerRO<Holder<Biome>> biomes = factory.biomeContainerCodec().parse(NbtOps.INSTANCE, biomeTag).getOrThrow();
                    if (section.getStates().bitsPerEntry() == 0 && section.getBiomes().bitsPerEntry() == 0) {
                        singleValue++;
                    }
                    if (!sameContents(section, blocks, biomes)) {
                        mismatches++;
                    }
                }
            }
        }
        return new int[] {sections, singleValue, mismatches};
    }

    private static boolean sameContents(LevelChunkSection section, PalettedContainer<BlockState> blocks, PalettedContainerRO<Holder<Biome>> biomes) {
        for (int y = 0; y < SECTION_SIZE; y++) {
            for (int z = 0; z < SECTION_SIZE; z++) {
                for (int x = 0; x < SECTION_SIZE; x++) {
                    if (section.getBlockState(x, y, z) != blocks.get(x, y, z)) {
                        return false;
                    }
                }
            }
        }
        for (int y = 0; y < QUARTS_PER_SECTION; y++) {
            for (int z = 0; z < QUARTS_PER_SECTION; z++) {
                for (int x = 0; x < QUARTS_PER_SECTION; x++) {
                    if (!section.getNoiseBiome(x, y, z).equals(biomes.get(x, y, z))) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /** CPU time of the whole server process, so a result does not depend on idle waits or short-lived threads. */
    private static long processCpuNanos() {
        return ManagementFactory.getOperatingSystemMXBean() instanceof OperatingSystemMXBean os ? os.getProcessCpuTime() : 0L;
    }

    private static int radius() {
        return Integer.getInteger(RADIUS_PROPERTY, DEFAULT_RADIUS);
    }

    private static void onServerStarted(ServerStartedEvent event) {
        ServerLevel level = event.getServer().overworld();
        // Frozen ticks keep chunks loading and generating but stop water flow, falling sand, random ticks and mobs.
        event.getServer().tickRateManager().setFrozen(true);
        ServerLightTimings.take();
        startNanos = System.nanoTime();
        startCpuNanos = processCpuNanos();
        int radius = radius();
        for (int chunkX = -radius; chunkX <= radius; chunkX++) {
            for (int chunkZ = -radius; chunkZ <= radius; chunkZ++) {
                level.setChunkForced(chunkX, chunkZ, true);
            }
        }
        running = true;
        Aervium.LOGGER.info("Aervium worldgen check: generating {} chunks", (2 * radius + 1) * (2 * radius + 1));
    }

    private static void onServerTickEnd(ServerTickEvent.Post event) {
        if (!running) {
            return;
        }
        MinecraftServer server = event.getServer();
        ServerLevel level = server.overworld();
        int radius = radius();
        for (int chunkX = -radius; chunkX <= radius; chunkX++) {
            for (int chunkZ = -radius; chunkZ <= radius; chunkZ++) {
                if (level.getChunkSource().getChunkNow(chunkX, chunkZ) == null) {
                    return;
                }
            }
        }
        running = false;
        double generationMs = (System.nanoTime() - startNanos) / NANOS_PER_MILLI;
        double cpuMs = (processCpuNanos() - startCpuNanos) / NANOS_PER_MILLI;
        ServerLightTimings.Totals light = ServerLightTimings.take();
        StateHash terrain = new StateHash();
        int missing = 0;
        for (int chunkX = -radius; chunkX <= radius; chunkX++) {
            for (int chunkZ = -radius; chunkZ <= radius; chunkZ++) {
                Long chunkHash = TERRAIN_HASHES.get(ChunkPos.pack(chunkX, chunkZ));
                if (chunkHash == null) {
                    missing++;
                } else {
                    terrain.add(chunkHash);
                }
            }
        }
        Aervium.LOGGER.info("Aervium worldgen check: terrain {} missing {}", terrain.hex(), missing);
        int[] saveCheck = checkSaves(level, radius);
        Aervium.LOGGER.info("Aervium worldgen check: save round trip of {} sections ({} with a single value), {} mismatches",
            saveCheck[0], saveCheck[1], saveCheck[2]);
        Aervium.LOGGER.info("Aervium worldgen check: generated in {} ms, process {} ms CPU, light worker {} ms CPU",
            String.format(Locale.ROOT, "%.0f", generationMs), String.format(Locale.ROOT, "%.0f", cpuMs),
            String.format(Locale.ROOT, "%.0f", light.cpuMs()));
        server.halt(false);
    }
}
