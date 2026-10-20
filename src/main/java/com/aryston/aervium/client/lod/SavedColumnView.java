package com.aryston.aervium.client.lod;

import com.aryston.aervium.core.lod.LodColor;
import com.aryston.aervium.core.lod.LodSample;
import com.aryston.aervium.core.lod.LodLayers;
import com.aryston.aervium.core.lod.LodStore;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.CardinalLighting;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import org.jspecify.annotations.Nullable;

final class SavedColumnView implements BlockAndTintGetter {
    private static final int RGB_MASK = 0xFFFFFF;
    private static final int SECTION_MASK = LodStore.CHUNK_SIZE - 1;
    private static final int MAX_WATER_DEPTH = 255;
    private static final int SHADED_SKY_LIGHT = 8;
    private static final int SOLID_GROUND_THICKNESS = 12;

    private final ChunkPos chunkPos;
    private final LevelChunkSection[] sections;
    private final LevelHeightAccessor heights;
    private final BlockColors colors;
    private final int sourceFlag;
    private final DataLayer[] sky;
    private final DataLayer[] block;
    private final @Nullable LevelLightEngine clientLight;
    private final boolean hasSky;
    private final BlockPos.MutableBlockPos position = new BlockPos.MutableBlockPos();

    SavedColumnView(ChunkAccess chunk, BlockColors colors, int sourceFlag, boolean hasSky) {
        chunkPos = chunk.getPos();
        heights = LevelHeightAccessor.create(chunk.getMinY(), chunk.getHeight());
        sections = chunk.getSections();
        this.colors = colors;
        this.sourceFlag = sourceFlag;
        this.hasSky = hasSky;
        clientLight = chunk instanceof LevelChunk client && sourceFlag == 0 ? client.getLevel().getLightEngine() : null;
        sky = new DataLayer[0];
        block = new DataLayer[0];
    }

    private static boolean isVisible(BlockState state) {
        return !state.isAir() && (state.is(BlockTags.BLOCKS_MOTION_IN_HEIGHTMAP) || !state.getFluidState().isEmpty() || state.is(Blocks.SNOW));
    }

    long sample(int localX, int localZ) {
        int blockX = chunkPos.getMinBlockX() + localX;
        int blockZ = chunkPos.getMinBlockZ() + localZ;
        for (int index = sections.length - 1; index >= 0; index--) {
            LevelChunkSection section = sections[index];
            if (section == null || section.hasOnlyAir()) {
                continue;
            }
            for (int y = SECTION_MASK; y >= 0; y--) {
                BlockState state = section.getBlockState(localX, y, localZ);
                if (!isVisible(state)) {
                    continue;
                }
                int top = getMinY() + index * LodStore.CHUNK_SIZE + y;
                position.set(blockX, top, blockZ);
                int rgb = TerrainMaterials.color(state, this, position, colors, false);
                boolean water = state.getFluidState().is(FluidTags.WATER);
                int depth = water ? waterDepth(top) : 0;
                position.setY(top + 1);
                return light(LodSample.of(top, rgb, depth, sourceFlag | (water ? LodSample.FLAG_WATER : 0)), true);
            }
        }
        return LodSample.of(Short.MIN_VALUE, 0, 0, LodSample.FLAG_VOID | sourceFlag);
    }

    boolean isolated() {
        return clientLight == null;
    }

    void layers(int localX, int localZ, LodLayers.Builder layers) {
        position.set(chunkPos.getMinBlockX() + localX, getMinY() + getHeight() - 1, chunkPos.getMinBlockZ() + localZ);
        int count = 0;
        int y = position.getY();
        while (y >= getMinY() && count < LodLayers.MAX_LAYERS) {
            int sectionIndex = heights.getSectionIndex(y);
            if (sectionIndex < 0 || sectionIndex >= sections.length) {
                break;
            }
            LevelChunkSection section = sections[sectionIndex];
            if (section == null || section.hasOnlyAir()) {
                y = getMinY() + sectionIndex * LodStore.CHUNK_SIZE - 1;
                continue;
            }
            position.setY(y);
            BlockState state = section.getBlockState(localX, y & SECTION_MASK, localZ);
            if (!isVisible(state)) {
                y--;
                continue;
            }
            int top = y;
            boolean water = state.getFluidState().is(FluidTags.WATER);
            boolean foliage = state.is(BlockTags.LEAVES);
            boolean trunk = state.is(BlockTags.LOGS);
            int rgb = TerrainMaterials.color(state, this, position, colors, false);
            int side = TerrainMaterials.color(state, this, position, colors, true);
            if (state.is(Blocks.GRASS_BLOCK)) {
                side = LodColor.tint(side, rgb);
            }
            while (y > getMinY()) {
                position.setY(y - 1);
                BlockState next = getBlockState(position);
                if (!isVisible(next)
                    || next.getFluidState().is(FluidTags.WATER) != water
                    || next.is(BlockTags.LEAVES) != foliage
                    || next.is(BlockTags.LOGS) != trunk) {
                    break;
                }
                y--;
                if (!water && !foliage && !trunk && top - y + 1 >= SOLID_GROUND_THICKNESS) {
                    break;
                }
            }
            count++;
            boolean ground = !water && !foliage && !trunk && top - y + 1 >= SOLID_GROUND_THICKNESS;
            int bottom = count == LodLayers.MAX_LAYERS || ground ? getMinY() : y;
            position.setY(top + 1);
            long surface = light(LodSample.of(top, rgb, water ? top - y + 1 : 0, sourceFlag | (water ? LodSample.FLAG_WATER : 0)), count == 1);
            layers.add(surface, bottom, side);
            if (ground) {
                break;
            }
            y--;
        }
    }

    private long light(long sample, boolean exposed) {
        BlockPos surface = position.below();
        int emission = getBlockState(surface).getLightEmission(this, surface);
        if (clientLight != null) {
            return LodSample.withLight(sample, clientLight.getLayerListener(LightLayer.SKY).getLightValue(position),
                Math.max(emission, clientLight.getLayerListener(LightLayer.BLOCK).getLightValue(position)));
        }
        int index = heights.getSectionIndex(position.getY()) + 1;
        DataLayer skyLayer = index >= 0 && index < sky.length ? sky[index] : null;
        DataLayer blockLayer = index >= 0 && index < block.length ? block[index] : null;
        int x = position.getX() & SECTION_MASK;
        int y = position.getY() & SECTION_MASK;
        int z = position.getZ() & SECTION_MASK;
        int defaultSky = hasSky ? exposed ? LodSample.MAX_LIGHT : SHADED_SKY_LIGHT : 0;
        return LodSample.withLight(sample, skyLayer == null ? defaultSky : skyLayer.get(x, y, z),
            Math.max(emission, blockLayer == null ? 0 : blockLayer.get(x, y, z)));
    }

    private int waterDepth(int top) {
        int depth = 1;
        while (depth < MAX_WATER_DEPTH && top - depth >= getMinY()) {
            position.setY(top - depth);
            if (!getFluidState(position).is(FluidTags.WATER)) {
                break;
            }
            depth++;
        }
        return depth;
    }

    @Override
    public CardinalLighting cardinalLighting() {
        return CardinalLighting.DEFAULT;
    }

    @Override
    public LevelLightEngine getLightEngine() {
        return LevelLightEngine.EMPTY;
    }

    @Override
    public int getBlockTint(BlockPos pos, ColorResolver resolver) {
        int index = Math.clamp(heights.getSectionIndex(pos.getY()), 0, sections.length - 1);
        LevelChunkSection section = sections[index];
        if (section == null) {
            return RGB_MASK;
        }
        return resolver.getColor(section.getNoiseBiome(
            QuartPos.fromBlock(pos.getX() & SECTION_MASK), QuartPos.fromBlock(pos.getY() & SECTION_MASK), QuartPos.fromBlock(pos.getZ() & SECTION_MASK)
        ).value(), pos.getX(), pos.getZ());
    }

    @Override
    public @Nullable BlockEntity getBlockEntity(BlockPos pos) {
        return null;
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        int index = heights.getSectionIndex(pos.getY());
        if (index < 0 || index >= sections.length || sections[index] == null
            || Math.floorDiv(pos.getX(), LodStore.CHUNK_SIZE) != chunkPos.x()
            || Math.floorDiv(pos.getZ(), LodStore.CHUNK_SIZE) != chunkPos.z()) {
            return Blocks.AIR.defaultBlockState();
        }
        return sections[index].getBlockState(pos.getX() & SECTION_MASK, pos.getY() & SECTION_MASK, pos.getZ() & SECTION_MASK);
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        return getBlockState(pos).getFluidState();
    }

    @Override
    public int getHeight() {
        return heights.getHeight();
    }

    @Override
    public int getMinY() {
        return heights.getMinY();
    }
}
