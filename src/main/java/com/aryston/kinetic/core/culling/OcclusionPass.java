package com.aryston.kinetic.core.culling;

import java.util.Arrays;

public final class OcclusionPass {
    public static final float SECTION_SIZE = 16.0F;
    public static final float GEOMETRY_MARGIN = 1.0F;
    public static final float MAX_REVEAL_REACH = 8.0F;
    private static final float[] REVEAL_STEPS = {1.0F, 2.0F, 4.0F, MAX_REVEAL_REACH};
    private static final int INITIAL_CAPACITY = 1024;

    private float[] originX = new float[INITIAL_CAPACITY];
    private float[] originY = new float[INITIAL_CAPACITY];
    private float[] originZ = new float[INITIAL_CAPACITY];
    private OccluderBoxes[] boxes = new OccluderBoxes[INITIAL_CAPACITY];
    private boolean[] canOcclude = new boolean[INITIAL_CAPACITY];
    private boolean[] visible = new boolean[INITIAL_CAPACITY];
    private float[] hiddenUpTo = new float[INITIAL_CAPACITY];
    private int[] revealed = new int[INITIAL_CAPACITY];
    private long[] order = new long[INITIAL_CAPACITY];
    private int size;
    private int hidden;
    private int occluderBoxes;

    public void clear() {
        Arrays.fill(boxes, 0, size, null);
        size = 0;
        hidden = 0;
        occluderBoxes = 0;
    }

    public int add(float x, float y, float z, OccluderBoxes sectionBoxes, boolean occluder) {
        if (size == originX.length) {
            grow();
        }
        int index = size++;
        originX[index] = x;
        originY[index] = y;
        originZ[index] = z;
        boxes[index] = sectionBoxes;
        canOcclude[index] = occluder && !sectionBoxes.isEmpty();
        visible[index] = true;
        return index;
    }

    public int size() {
        return size;
    }

    public int hidden() {
        return hidden;
    }

    public int occluderBoxes() {
        return occluderBoxes;
    }

    public boolean isVisible(int index) {
        return visible[index];
    }

    public boolean occludes(int index) {
        return canOcclude[index];
    }

    public boolean sameOccluders(int index, OccluderBoxes sectionBoxes, boolean occluder) {
        return boxes[index] == sectionBoxes && canOcclude[index] == (occluder && !sectionBoxes.isEmpty());
    }

    public boolean sectionTiles(DepthRaster raster, int index, int[] range) {
        float x = originX[index];
        float y = originY[index];
        float z = originZ[index];
        return raster.tileRange(
            x - GEOMETRY_MARGIN, y - GEOMETRY_MARGIN, z - GEOMETRY_MARGIN,
            x + SECTION_SIZE + GEOMETRY_MARGIN, y + SECTION_SIZE + GEOMETRY_MARGIN, z + SECTION_SIZE + GEOMETRY_MARGIN,
            range
        );
    }

    public void run(DepthRaster raster, int maxOccluderBoxes) {
        run(raster, maxOccluderBoxes, 0.0F);
    }

    public void run(DepthRaster raster, int maxOccluderBoxes, float reach) {
        ClipMatrix clip = raster.matrix();
        float half = SECTION_SIZE * 0.5F;
        for (int index = 0; index < size; index++) {
            float x = originX[index] + half;
            float y = originY[index] + half;
            float z = originZ[index] + half;
            float w = clip.w(x, y, z);
            float depth = w > 0.0F ? Math.max(0.0F, clip.z(x, y, z) / w) : 0.0F;
            order[index] = (long) Float.floatToIntBits(depth) << Integer.SIZE | index;
        }
        Arrays.sort(order, 0, size);
        int budget = maxOccluderBoxes;
        for (int rank = 0; rank < size; rank++) {
            int index = (int) order[rank];
            if (!visibleWithin(raster, index, reach)) {
                visible[index] = false;
                hidden++;
                continue;
            }
            if (!canOcclude[index] || budget <= 0) {
                continue;
            }
            float x = originX[index];
            float y = originY[index];
            float z = originZ[index];
            OccluderBoxes section = boxes[index];
            for (int box = 0; box < section.count() && budget > 0; box++) {
                raster.addOccluder(
                    x + section.minX(box), y + section.minY(box), z + section.minZ(box),
                    x + section.maxX(box), y + section.maxY(box), z + section.maxZ(box)
                );
                budget--;
                occluderBoxes++;
            }
        }
    }

    public void measureReveal(DepthRaster raster, float reach) {
        for (int index = 0; index < size; index++) {
            if (visible[index]) {
                continue;
            }
            float upTo = reach;
            for (int step = REVEAL_STEPS.length - 1; step >= 0 && REVEAL_STEPS[step] > reach; step--) {
                if (!visibleWithin(raster, index, REVEAL_STEPS[step])) {
                    upTo = REVEAL_STEPS[step];
                    break;
                }
            }
            hiddenUpTo[index] = upTo;
        }
    }

    public int widen(float reach) {
        int count = 0;
        for (int index = 0; index < size; index++) {
            if (!visible[index] && reach > hiddenUpTo[index]) {
                visible[index] = true;
                hidden--;
                revealed[count++] = index;
            }
        }
        return count;
    }

    public int revealed(int rank) {
        return revealed[rank];
    }

    private boolean visibleWithin(DepthRaster raster, int index, float reach) {
        float margin = GEOMETRY_MARGIN + reach;
        float x = originX[index];
        float y = originY[index];
        float z = originZ[index];
        return raster.isVisible(
            x - margin, y - margin, z - margin,
            x + SECTION_SIZE + margin, y + SECTION_SIZE + margin, z + SECTION_SIZE + margin
        );
    }

    private void grow() {
        int capacity = originX.length * 2;
        originX = Arrays.copyOf(originX, capacity);
        originY = Arrays.copyOf(originY, capacity);
        originZ = Arrays.copyOf(originZ, capacity);
        boxes = Arrays.copyOf(boxes, capacity);
        canOcclude = Arrays.copyOf(canOcclude, capacity);
        visible = Arrays.copyOf(visible, capacity);
        hiddenUpTo = Arrays.copyOf(hiddenUpTo, capacity);
        revealed = Arrays.copyOf(revealed, capacity);
        order = Arrays.copyOf(order, capacity);
    }
}
