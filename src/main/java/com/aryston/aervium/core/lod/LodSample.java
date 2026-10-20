package com.aryston.aervium.core.lod;

public final class LodSample {
    public static final long EMPTY = 0L;
    public static final int FLAG_PRESENT = 1;
    public static final int FLAG_WATER = 1 << 1;
    public static final int FLAG_APPROXIMATE = 1 << 2;
    public static final int FLAG_PARTIAL = 1 << 3;
    public static final int FLAG_VOID = 1 << 4;
    public static final int FLAG_SEEDED = 1 << 5;
    public static final int FLAG_SAVED = 1 << 6;
    private static final int CLIENT_PRIORITY = 3;
    static final int SAVED_PRIORITY = 2;
    public static final int SEEDED_PRIORITY = 1;

    private static final int HEIGHT_SHIFT = 48;
    private static final int COLOR_SHIFT = 16;
    private static final int WATER_SHIFT = 8;
    private static final long COLOR_MASK = 0xFFFFFFL;
    private static final int LIGHT_SHIFT = 40;
    private static final int BLOCK_LIGHT_SHIFT = 4;
    private static final int VERTEX_BLOCK_MAX = 7;
    private static final int VERTEX_BLOCK_SHIFT = 1;
    public static final int MAX_LIGHT = 15;
    private static final int BYTE_MASK = 0xFF;
    private static final int MAX_WATER_DEPTH = 255;

    private LodSample() {
    }

    public static long of(int height, int rgb, int waterDepth, int flags) {
        return ((long) (short) height << HEIGHT_SHIFT)
            | ((rgb & COLOR_MASK) << COLOR_SHIFT)
            | ((long) MAX_LIGHT << LIGHT_SHIFT)
            | ((long) Math.clamp(waterDepth, 0, MAX_WATER_DEPTH) << WATER_SHIFT)
            | ((flags | FLAG_PRESENT) & BYTE_MASK);
    }

    public static boolean present(long sample) {
        return (sample & FLAG_PRESENT) != 0;
    }

    public static boolean water(long sample) {
        return (sample & FLAG_WATER) != 0;
    }

    public static boolean complete(long sample) {
        return present(sample) && (sample & FLAG_PARTIAL) == 0;
    }

    public static boolean approximate(long sample) {
        return (sample & FLAG_APPROXIMATE) != 0;
    }

    public static boolean visible(long sample) {
        return present(sample) && (sample & FLAG_VOID) == 0;
    }

    public static int priority(long sample) {
        if (!present(sample) || approximate(sample)) {
            return 0;
        }
        return (sample & FLAG_SEEDED) != 0 ? SEEDED_PRIORITY : (sample & FLAG_SAVED) != 0 ? SAVED_PRIORITY : CLIENT_PRIORITY;
    }

    public static int height(long sample) {
        return (short) (sample >> HEIGHT_SHIFT);
    }

    public static int rgb(long sample) {
        return (int) ((sample >>> COLOR_SHIFT) & COLOR_MASK);
    }

    public static long withColor(long sample, int color) {
        return (sample & ~(COLOR_MASK << COLOR_SHIFT)) | ((color & COLOR_MASK) << COLOR_SHIFT);
    }

    public static int waterDepth(long sample) {
        return (int) ((sample >>> WATER_SHIFT) & BYTE_MASK);
    }

    public static int flags(long sample) {
        return (int) (sample & BYTE_MASK);
    }

    public static long withLight(long sample, int sky, int block) {
        int light = Math.clamp(sky, 0, MAX_LIGHT) | Math.clamp(block, 0, MAX_LIGHT) << BLOCK_LIGHT_SHIFT;
        return (sample & ~((long) BYTE_MASK << LIGHT_SHIFT)) | ((long) light << LIGHT_SHIFT);
    }

    public static int skyLight(long sample) {
        return (int) (sample >>> LIGHT_SHIFT) & MAX_LIGHT;
    }

    public static int blockLight(long sample) {
        return (int) (sample >>> (LIGHT_SHIFT + BLOCK_LIGHT_SHIFT)) & MAX_LIGHT;
    }

    public static byte vertexLight(long sample) {
        return (byte) (skyLight(sample) << BLOCK_LIGHT_SHIFT
            | ((blockLight(sample) * VERTEX_BLOCK_MAX + VERTEX_BLOCK_MAX) / MAX_LIGHT) << VERTEX_BLOCK_SHIFT | (water(sample) ? FLAG_PRESENT : 0));
    }
}
