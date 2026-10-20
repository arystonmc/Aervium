package com.aryston.aervium.client.lod;

import com.aryston.aervium.core.lod.LodSample;
import com.aryston.aervium.core.lod.LodData;
import com.aryston.aervium.core.lod.LodLayers;
import com.aryston.aervium.core.lod.LodStore;
import com.aryston.aervium.core.lod.LodTile;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.Tags;

final class GeneratedColumns {
    private static final int RGB_MASK = 0xFFFFFF;
    private static final int CHANNEL_MASK = 0xFF;
    private static final int RED_SHIFT = 16;
    private static final int GREEN_SHIFT = 8;
    private static final int CANOPY_HEIGHT = 5;
    private static final int BEACH_MARGIN = 2;
    private static final double CLIFF_SLOPE = 1.15;
    private static final double HALF = 0.5;
    private static final int HASH_X = 0x9E3779B9;
    private static final int HASH_Z = 0x85EBCA6B;
    private static final int HASH_MIX = 0xC2B2AE35;

    private final ChunkGenerator generator;
    private final RandomState randomState;
    private final LevelHeightAccessor heights;
    private final BiomeResolver biomes;
    private final int seaLevel;
    private final TerrainCache cache;

    private GeneratedColumns(ServerLevel level) {
        generator = level.getChunkSource().getGenerator();
        randomState = level.getChunkSource().randomState();
        heights = LevelHeightAccessor.create(level.getMinY(), level.getHeight());
        biomes = generator.getBiomeSource().createUncachedResolver(randomState);
        seaLevel = generator.getSeaLevel();
        cache = new TerrainCache(level);
    }

    static GeneratedColumns of(ServerLevel level) {
        return new GeneratedColumns(level);
    }

    LodData tile(int level, int tileX, int tileZ, int grid, BooleanSupplier cancelled) {
        long key = LodStore.key(level, tileX, tileZ);
        LodData cached = cache.read(key, false);
        long[] samples = cached == null ? new long[LodTile.SAMPLES] : cached.samples();
        LodLayers layers = cached == null ? null : cached.layers();
        boolean complete = true;
        for (long sample : samples) {
            complete &= LodSample.present(sample);
        }
        if (complete) {
            return new LodData(samples, layers);
        }
        int step = 1 << level;
        int tileBlocks = LodTile.SIZE * step;
        int originX = tileX * tileBlocks;
        int originZ = tileZ * tileBlocks;
        double spacing = (double) tileBlocks / grid;
        int points = grid + 1;
        int[] floors = new int[points * points];
        Surface[] surfaces = new Surface[points * points];
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int gz = 0; gz < points; gz++) {
            if (cancelled.getAsBoolean()) {
                throw new CancellationException();
            }
            for (int gx = 0; gx < points; gx++) {
                int blockX = originX + (int) Math.round(gx * spacing);
                int blockZ = originZ + (int) Math.round(gz * spacing);
                int floor = generator.getBaseHeight(blockX, blockZ, Heightmap.Types.OCEAN_FLOOR_WG, heights, randomState) - 1;
                floors[gz * points + gx] = floor;
                surfaces[gz * points + gx] = surface(blockX, blockZ, floor, pos);
            }
        }
        for (int z = 0; z < LodTile.SIZE; z++) {
            if (cancelled.getAsBoolean()) {
                throw new CancellationException();
            }
            double gridZ = (z * step + step * HALF) / spacing;
            for (int x = 0; x < LodTile.SIZE; x++) {
                if (LodSample.present(samples[z * LodTile.SIZE + x])) {
                    continue;
                }
                double gridX = (x * step + step * HALF) / spacing;
                int columnX = originX + x * step;
                int columnZ = originZ + z * step;
                samples[z * LodTile.SIZE + x] = sample(floors, surfaces, points, spacing, gridX, gridZ, columnX, columnZ);
            }
        }
        LodData result = new LodData(samples, layers);
        if (!hasSavedSamples(samples) && !cancelled.getAsBoolean()) {
            cache.write(key, result);
        }
        return result;
    }

    private static boolean hasSavedSamples(long[] samples) {
        for (long sample : samples) {
            if ((sample & LodSample.FLAG_SAVED) != 0) {
                return true;
            }
        }
        return false;
    }

    private long sample(int[] floors, Surface[] surfaces, int points, double spacing, double gridX, double gridZ, int blockX, int blockZ) {
        int cellX = Math.min((int) gridX, points - 2);
        int cellZ = Math.min((int) gridZ, points - 2);
        double fractionX = gridX - cellX;
        double fractionZ = gridZ - cellZ;
        int corner = cellZ * points + cellX;
        int f00 = floors[corner];
        int f10 = floors[corner + 1];
        int f01 = floors[corner + points];
        int f11 = floors[corner + points + 1];
        double top = lerp(fractionX, f00, f10);
        double bottom = lerp(fractionX, f01, f11);
        int floor = (int) Math.round(lerp(fractionZ, top, bottom));
        if (floor < heights.getMinY()) {
            return LodSample.of(Short.MIN_VALUE, 0, 0, LodSample.FLAG_VOID | LodSample.FLAG_APPROXIMATE);
        }
        Surface s00 = surfaces[corner];
        Surface s10 = surfaces[corner + 1];
        Surface s01 = surfaces[corner + points];
        Surface s11 = surfaces[corner + points + 1];
        int water = seaLevel - 1;
        if (floor < water) {
            int waterColor = bilerpColor(fractionX, fractionZ, s00.water(), s10.water(), s01.water(), s11.water());
            return LodSample.of(water, waterColor, water - floor, LodSample.FLAG_WATER | LodSample.FLAG_APPROXIMATE);
        }
        int landColor = bilerpColor(fractionX, fractionZ, s00.land(), s10.land(), s01.land(), s11.land());
        int relief = Math.max(Math.max(f00, f10), Math.max(f01, f11)) - Math.min(Math.min(f00, f10), Math.min(f01, f11));
        double canopyWeight = lerp(fractionZ, lerp(fractionX, s00.canopy(), s10.canopy()), lerp(fractionX, s01.canopy(), s11.canopy()));
        int hash = columnHash(blockX, blockZ);
        if (relief > spacing * CLIFF_SLOPE) {
            double steepness = Math.min(1.0, (relief - spacing * CLIFF_SLOPE) / Math.max(4.0, spacing));
            landColor = lerpColor(steepness, landColor, TerrainMaterials.topColor(Blocks.STONE.defaultBlockState()));
            canopyWeight *= 1.0 - steepness;
        } else if (floor <= water + BEACH_MARGIN && (s00.beach() || s10.beach() || s01.beach() || s11.beach())) {
            double coast = 1.0 - (double) (floor - water) / (BEACH_MARGIN + 1);
            landColor = lerpColor(coast * 0.85, landColor, TerrainMaterials.topColor(Blocks.SAND.defaultBlockState()));
            canopyWeight = 0.0;
        }
        return LodSample.of(floor, landColor, 0, LodSample.FLAG_APPROXIMATE);
    }

    private Surface surface(int blockX, int blockZ, int floor, BlockPos.MutableBlockPos pos) {
        int water = seaLevel - 1;
        Holder<Biome> biome = biomes.getNoiseBiome(QuartPos.fromBlock(blockX), QuartPos.fromBlock(Math.max(floor, water)), QuartPos.fromBlock(blockZ));
        pos.set(blockX, Math.max(floor, water), blockZ);
        boolean wooded = biome.is(BiomeTags.IS_FOREST) || biome.is(BiomeTags.IS_TAIGA) || biome.is(BiomeTags.IS_JUNGLE);
        boolean snow = biome.value().coldEnoughToSnow(pos, seaLevel);
        boolean beach = !snow && !biome.is(BiomeTags.IS_BADLANDS) && !biome.is(Tags.Biomes.IS_SWAMP) && !biome.is(Tags.Biomes.IS_MUSHROOM);
        int waterColor = TerrainMaterials.tintedTopColor(Blocks.WATER.defaultBlockState(), biome.value().getWaterColor() & RGB_MASK);
        return new Surface(landColour(biome, wooded, snow, blockX, blockZ) & RGB_MASK, waterColor,
            wooded && floor > water + 1 ? CANOPY_HEIGHT : 0, beach);
    }

    private static int landColour(Holder<Biome> biome, boolean wooded, boolean snow, int blockX, int blockZ) {
        if (biome.is(BiomeTags.IS_BADLANDS)) {
            return TerrainMaterials.topColor(Blocks.TERRACOTTA.defaultBlockState());
        }
        if (biome.is(Tags.Biomes.IS_MUSHROOM)) {
            return TerrainMaterials.topColor(Blocks.MYCELIUM.defaultBlockState());
        }
        if (snow) {
            return TerrainMaterials.topColor(Blocks.SNOW_BLOCK.defaultBlockState());
        }
        if (biome.is(Tags.Biomes.IS_DESERT) || biome.is(BiomeTags.IS_BEACH)) {
            return TerrainMaterials.topColor(Blocks.SAND.defaultBlockState());
        }
        int grassColor = TerrainMaterials.tintedTopColor(Blocks.GRASS_BLOCK.defaultBlockState(), biome.value().getGrassColor(blockX, blockZ) & RGB_MASK);
        if (wooded) {
            int foliageColor = TerrainMaterials.tintedTopColor(Blocks.OAK_LEAVES.defaultBlockState(), biome.value().getFoliageColor() & RGB_MASK);
            return lerpColor(0.65, grassColor, foliageColor);
        }
        return grassColor;
    }

    private static int bilerpColor(double fx, double fz, int c00, int c10, int c01, int c11) {
        return lerpColor(fz, lerpColor(fx, c00, c10), lerpColor(fx, c01, c11));
    }

    private static int lerpColor(double fraction, int from, int to) {
        int r = (int) Math.round(lerp(fraction, (from >>> RED_SHIFT) & CHANNEL_MASK, (to >>> RED_SHIFT) & CHANNEL_MASK));
        int g = (int) Math.round(lerp(fraction, (from >>> GREEN_SHIFT) & CHANNEL_MASK, (to >>> GREEN_SHIFT) & CHANNEL_MASK));
        int b = (int) Math.round(lerp(fraction, from & CHANNEL_MASK, to & CHANNEL_MASK));
        return (Math.clamp(r, 0, CHANNEL_MASK) << RED_SHIFT) | (Math.clamp(g, 0, CHANNEL_MASK) << GREEN_SHIFT) | Math.clamp(b, 0, CHANNEL_MASK);
    }

    private static int scaleColor(int rgb, int percent) {
        int r = Math.clamp(((rgb >>> RED_SHIFT) & CHANNEL_MASK) * percent / 100, 0, CHANNEL_MASK);
        int g = Math.clamp(((rgb >>> GREEN_SHIFT) & CHANNEL_MASK) * percent / 100, 0, CHANNEL_MASK);
        int b = Math.clamp((rgb & CHANNEL_MASK) * percent / 100, 0, CHANNEL_MASK);
        return (r << RED_SHIFT) | (g << GREEN_SHIFT) | b;
    }

    private static int columnHash(int x, int z) {
        int h = x * HASH_X ^ z * HASH_Z;
        h = (h ^ (h >>> 16)) * HASH_MIX;
        return h ^ (h >>> 16);
    }

    private static double lerp(double fraction, double from, double to) {
        return from + (to - from) * fraction;
    }

    private record Surface(int land, int water, int canopy, boolean beach) {
    }
}
