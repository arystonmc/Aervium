package com.aryston.kinetic.core.lod;

public final class LodCombine {
    private static final int CHANNEL_MASK = 0xFF;
    private static final int RED_SHIFT = 16;
    private static final int GREEN_SHIFT = 8;
    private static final int MAJORITY = 2;
    private static final int CELLS = 4;

    private LodCombine() {
    }

    public static long of(long[] samples, int corner, int rowWidth) {
        int present = 0;
        int water = 0;
        int visible = 0;
        int height = Short.MIN_VALUE;
        int waterDepth = 0;
        int red = 0;
        int green = 0;
        int blue = 0;
        int sky = 0;
        int block = 0;
        int inheritedFlags = 0;
        for (int cell = 0; cell < CELLS; cell++) {
            long sample = samples[corner + (cell >> 1) * rowWidth + (cell & 1)];
            if (!LodSample.present(sample)) {
                continue;
            }
            present++;
            inheritedFlags |= LodSample.flags(sample) & (LodSample.FLAG_APPROXIMATE | LodSample.FLAG_PARTIAL | LodSample.FLAG_SEEDED | LodSample.FLAG_SAVED);
            if (!LodSample.visible(sample)) {
                continue;
            }
            visible++;
            sky += LodSample.skyLight(sample);
            block = Math.max(block, LodSample.blockLight(sample));
            height = Math.max(height, LodSample.height(sample));
            if (LodSample.water(sample)) {
                water++;
                waterDepth = Math.max(waterDepth, LodSample.waterDepth(sample));
            }
            int rgb = LodSample.rgb(sample);
            red += (rgb >> RED_SHIFT) & CHANNEL_MASK;
            green += (rgb >> GREEN_SHIFT) & CHANNEL_MASK;
            blue += rgb & CHANNEL_MASK;
        }
        if (present == 0) {
            return LodSample.EMPTY;
        }
        int partialFlag = present < CELLS ? LodSample.FLAG_PARTIAL : 0;
        if (visible == 0) {
            return LodSample.of(Short.MIN_VALUE, 0, 0, LodSample.FLAG_VOID | partialFlag | inheritedFlags);
        }
        int rgb = (red / visible) << RED_SHIFT | (green / visible) << GREEN_SHIFT | blue / visible;
        int waterFlag = water >= MAJORITY || water == visible ? LodSample.FLAG_WATER : 0;
        return LodSample.withLight(LodSample.of(height, rgb, waterFlag == 0 ? 0 : waterDepth, waterFlag | partialFlag | inheritedFlags), sky / visible, block);
    }
}
