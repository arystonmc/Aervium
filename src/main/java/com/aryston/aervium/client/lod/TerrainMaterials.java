package com.aryston.aervium.client.lod;

import com.aryston.aervium.Aervium;
import com.aryston.aervium.core.lod.LodColor;
import com.mojang.blaze3d.platform.NativeImage;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.jspecify.annotations.Nullable;

final class TerrainMaterials {
    private static final long BUILD_BUDGET_NANOS = 4_000_000L;
    private static final int MAX_STATES_PER_TICK = 4096;
    private static final int SAMPLE_GRID = 8;
    private static final int RGB_MASK = 0xFFFFFF;
    private static final int CHANNEL_MASK = 0xFF;
    private static final int ALPHA_SHIFT = 24;
    private static final int RED_SHIFT = 16;
    private static final int GREEN_SHIFT = 8;
    private static final int NO_TINT = -1;
    private static final int WATER_TINT = -2;
    private static final double GAMMA = 2.2;
    private static final long MODEL_SEED = 0L;
    private static final int MATERIAL_WORDS = 5;

    private static final IdentityHashMap<TextureAtlasSprite, Integer> AVERAGES = new IdentityHashMap<>();
    private static final List<BlockStateModelPart> PARTS = new ArrayList<>();
    private static final RandomSource RANDOM = RandomSource.create(MODEL_SEED);
    private static volatile @Nullable Material[] palette;
    private static @Nullable Material[] building;
    private static int next;
    private static boolean failed;

    private TerrainMaterials() {
    }

    static boolean ready() {
        return palette != null;
    }

    static Consumer<MessageDigest> fingerprintSnapshot() {
        Material[] snapshot = palette;
        return digest -> {
            ByteBuffer bytes = ByteBuffer.allocate(MATERIAL_WORDS * Integer.BYTES);
            if (snapshot == null) {
                digest.update(bytes.array());
                return;
            }
            for (Material material : snapshot) {
                bytes.clear();
                if (material == null) {
                    bytes.putInt(0).putInt(0).putInt(0).putInt(0).putInt(0);
                } else {
                    bytes.putInt(1).putInt(material.top()).putInt(material.side()).putInt(material.topTint()).putInt(material.sideTint());
                }
                digest.update(bytes.array());
            }
        };
    }

    static void invalidate() {
        palette = null;
        building = null;
        next = 0;
        failed = false;
        AVERAGES.clear();
        PARTS.clear();
    }

    static void prepare(Minecraft minecraft) {
        if (palette != null || failed || !minecraft.isGameLoadFinished()) {
            return;
        }
        Material[] values = building;
        if (values == null) {
            values = new Material[Block.BLOCK_STATE_REGISTRY.size()];
            building = values;
        }
        long deadline = System.nanoTime() + BUILD_BUDGET_NANOS;
        try {
            for (int count = 0; next < values.length && count < MAX_STATES_PER_TICK && System.nanoTime() < deadline; count++) {
                BlockState state = Block.BLOCK_STATE_REGISTRY.byId(next);
                values[next++] = material(minecraft, state);
            }
            if (next == values.length) {
                palette = values;
                building = null;
                AVERAGES.clear();
            }
        } catch (RuntimeException failure) {
            failed = true;
            palette = values;
            building = null;
            AVERAGES.clear();
            Aervium.LOGGER.warn("Aervium distant terrain uses map colours for unsupported block models", failure);
        }
    }

    static int topColor(BlockState state) {
        Material[] values = palette;
        int id = Block.getId(state);
        Material material = values != null && id >= 0 && id < values.length ? values[id] : null;
        return material == null ? state.getMapColor(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).col & RGB_MASK : material.top();
    }

    static int sideColor(BlockState state) {
        Material[] values = palette;
        int id = Block.getId(state);
        Material material = values != null && id >= 0 && id < values.length ? values[id] : null;
        return material == null ? state.getMapColor(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).col & RGB_MASK : material.side();
    }

    static int tintedTopColor(BlockState state, int tintColor) {
        return LodColor.tint(topColor(state), tintColor);
    }

    static int tintedSideColor(BlockState state, int tintColor) {
        return LodColor.tint(sideColor(state), tintColor);
    }

    static int color(BlockState state, BlockAndTintGetter view, BlockPos pos, BlockColors colors, boolean side) {
        Material[] values = palette;
        int id = Block.getId(state);
        Material material = values != null && id >= 0 && id < values.length ? values[id] : null;
        int base = material == null ? state.getMapColor(view, pos).col & RGB_MASK : side ? material.side() : material.top();
        int tintIndex = material == null ? NO_TINT : side ? material.sideTint() : material.topTint();
        if (tintIndex == WATER_TINT) {
            return LodColor.tint(base, BiomeColors.getAverageWaterColor(view, pos) & RGB_MASK);
        }
        var tint = tintIndex == NO_TINT ? null : colors.getTintSource(state, tintIndex);
        if (tint == null || view instanceof SavedColumnView saved && saved.isolated() && !tint.getClass().getName().startsWith("net.minecraft.")) {
            return base;
        }
        int tintColor = tint.colorInWorld(state, view, pos) & RGB_MASK;
        return LodColor.tint(base, tintColor);
    }

    private static @Nullable Material material(Minecraft minecraft, BlockState state) {
        FluidState fluid = state.getFluidState();
        if (!fluid.isEmpty()) {
            var fluidModel = minecraft.getModelManager().getFluidStateModelSet().get(fluid);
            int avg = average(fluidModel.stillMaterial().sprite());
            int tint = fluid.is(FluidTags.WATER) ? WATER_TINT : NO_TINT;
            return new Material(avg, avg, tint, tint);
        }
        var models = minecraft.getModelManager().getBlockStateModelSet();
        var model = models.get(state);
        RANDOM.setSeed(MODEL_SEED);
        PARTS.clear();
        model.collectParts(BlockAndTintGetter.EMPTY, BlockPos.ZERO, state, RANDOM, PARTS);
        BakedQuad top = null;
        BakedQuad side = null;
        for (BlockStateModelPart part : PARTS) {
            List<BakedQuad> up = part.getQuads(Direction.UP);
            List<BakedQuad> north = part.getQuads(Direction.NORTH);
            if (top == null && !up.isEmpty()) {
                top = up.getFirst();
            }
            if (side == null && !north.isEmpty()) {
                side = north.getFirst();
            }
            for (BakedQuad quad : part.getQuads(null)) {
                if (top == null && quad.direction() == Direction.UP) {
                    top = quad;
                }
                if (side == null && quad.direction().getAxis().isHorizontal()) {
                    side = quad;
                }
            }
        }
        if (top == null && side == null) {
            return null;
        }
        if (top == null) {
            top = side;
        }
        if (side == null) {
            side = top;
        }
        return new Material(average(top.materialInfo().sprite()), average(side.materialInfo().sprite()),
            top.materialInfo().tintIndex(), side.materialInfo().tintIndex());
    }

    private static int average(TextureAtlasSprite sprite) {
        return AVERAGES.computeIfAbsent(sprite, value -> {
            NativeImage image = value.contents().getOriginalImage();
            double red = 0.0;
            double green = 0.0;
            double blue = 0.0;
            double weight = 0.0;
            for (int z = 0; z < SAMPLE_GRID; z++) {
                for (int x = 0; x < SAMPLE_GRID; x++) {
                    int pixel = image.getPixel(x * image.getWidth() / SAMPLE_GRID, z * image.getHeight() / SAMPLE_GRID);
                    double alpha = (pixel >>> ALPHA_SHIFT) / (double) CHANNEL_MASK;
                    red += Math.pow(((pixel >> RED_SHIFT) & CHANNEL_MASK) / (double) CHANNEL_MASK, GAMMA) * alpha;
                    green += Math.pow(((pixel >> GREEN_SHIFT) & CHANNEL_MASK) / (double) CHANNEL_MASK, GAMMA) * alpha;
                    blue += Math.pow((pixel & CHANNEL_MASK) / (double) CHANNEL_MASK, GAMMA) * alpha;
                    weight += alpha;
                }
            }
            return weight == 0.0 ? RGB_MASK : channel(red / weight) << RED_SHIFT | channel(green / weight) << GREEN_SHIFT | channel(blue / weight);
        });
    }

    private static int channel(double linear) {
        return (int) Math.round(Math.pow(linear, 1.0 / GAMMA) * CHANNEL_MASK);
    }

    private record Material(int top, int side, int topTint, int sideTint) {
    }
}
