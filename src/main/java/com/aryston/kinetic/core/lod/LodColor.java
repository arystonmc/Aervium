package com.aryston.kinetic.core.lod;

public final class LodColor {
    private static final int CHANNEL_MASK = 0xFF;
    private static final int RED_SHIFT = 16;
    private static final int GREEN_SHIFT = 8;

    private LodColor() {
    }

    public static int tint(int color, int tint) {
        int red = ((color >> RED_SHIFT) & CHANNEL_MASK) * ((tint >> RED_SHIFT) & CHANNEL_MASK) / CHANNEL_MASK;
        int green = ((color >> GREEN_SHIFT) & CHANNEL_MASK) * ((tint >> GREEN_SHIFT) & CHANNEL_MASK) / CHANNEL_MASK;
        int blue = (color & CHANNEL_MASK) * (tint & CHANNEL_MASK) / CHANNEL_MASK;
        return red << RED_SHIFT | green << GREEN_SHIFT | blue;
    }
}
