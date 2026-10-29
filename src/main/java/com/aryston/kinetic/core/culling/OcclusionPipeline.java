package com.aryston.kinetic.core.culling;

import java.util.Arrays;
import java.util.concurrent.Executor;

public final class OcclusionPipeline {
    private final Executor executor;
    private final Frame first;
    private final Frame second;
    private Frame active;
    private Frame pending;
    private int generation;

    public OcclusionPipeline(Executor executor, int maxOccluderBoxes) {
        this.executor = executor;
        this.first = new Frame(maxOccluderBoxes);
        this.second = new Frame(maxOccluderBoxes);
    }

    public Frame activateFinished() {
        Frame finished = pending;
        if (finished == null || !finished.finished) {
            return null;
        }
        pending = null;
        if (finished.failed) {
            return null;
        }
        active = finished;
        return finished;
    }

    public Frame active() {
        return active;
    }

    public Frame begin(long frame, double eyeX, double eyeY, double eyeZ, float reach) {
        if (pending != null) {
            return null;
        }
        Frame next = active == first ? second : first;
        next.reset(++generation, frame, eyeX, eyeY, eyeZ, reach);
        return next;
    }

    public void submit(Frame next) {
        next.submittedNanos = System.nanoTime();
        pending = next;
        executor.execute(next::run);
    }

    public void clear() {
        active = null;
        if (pending != null) {
            pending.failed = true;
        }
    }

    public static final class Frame {
        private static final int INITIAL_CAPACITY = 1024;

        private final OcclusionPass pass = new OcclusionPass();
        private final ClipMatrix clip = new ClipMatrix();
        private final TileMask changes = new TileMask();
        private final int[] range = new int[4];
        private final int maxOccluderBoxes;
        private DepthRaster raster;
        private Object[] targets = new Object[INITIAL_CAPACITY];
        private int generation;
        private long frame;
        private double eyeX;
        private double eyeY;
        private double eyeZ;
        private float reach;
        private long jobNanos;
        private long submittedNanos;
        private volatile boolean finished;
        private volatile boolean failed;

        private Frame(int maxOccluderBoxes) {
            this.maxOccluderBoxes = maxOccluderBoxes;
        }

        private void reset(int nextGeneration, long nextFrame, double x, double y, double z, float nextReach) {
            Arrays.fill(targets, 0, pass.size(), null);
            pass.clear();
            generation = nextGeneration;
            frame = nextFrame;
            eyeX = x;
            eyeY = y;
            eyeZ = z;
            reach = nextReach;
            jobNanos = 0L;
            finished = false;
            failed = false;
        }

        public ClipMatrix clip() {
            return clip;
        }

        public void raster(int width, int height) {
            if (raster == null || raster.width() != width || raster.height() != height) {
                raster = new DepthRaster(width, height);
            }
        }

        public int add(Object target, double x, double y, double z, OccluderBoxes boxes, boolean occluder) {
            int index = pass.add((float) (x - eyeX), (float) (y - eyeY), (float) (z - eyeZ), boxes, occluder);
            if (index == targets.length) {
                targets = Arrays.copyOf(targets, targets.length * 2);
            }
            targets[index] = target;
            return index;
        }

        private void run() {
            long start = System.nanoTime();
            try {
                raster.begin(clip, false);
                pass.run(raster, maxOccluderBoxes, reach);
                pass.measureReveal(raster, reach);
            } catch (RuntimeException failure) {
                failed = true;
                throw failure;
            } finally {
                jobNanos = System.nanoTime() - start;
                finished = true;
            }
        }

        public int generation() {
            return generation;
        }

        public long frame() {
            return frame;
        }

        public float reach() {
            return reach;
        }

        public long jobNanos() {
            return jobNanos;
        }

        public long submittedNanos() {
            return submittedNanos;
        }

        public int size() {
            return pass.size();
        }

        public int hidden() {
            return pass.hidden();
        }

        public boolean isVisible(int index) {
            return pass.isVisible(index);
        }

        public int revealed(int rank) {
            return pass.revealed(rank);
        }

        public Object target(int index) {
            return targets[index];
        }

        public double eyeX() {
            return eyeX;
        }

        public double eyeY() {
            return eyeY;
        }

        public double eyeZ() {
            return eyeZ;
        }

        public boolean covers(double x, double y, double z) {
            return distanceTo(x, y, z) <= reach;
        }

        public double distanceTo(double x, double y, double z) {
            double dx = x - eyeX;
            double dy = y - eyeY;
            double dz = z - eyeZ;
            return Math.sqrt(dx * dx + dy * dy + dz * dz);
        }

        public boolean sameOccluders(int index, OccluderBoxes boxes, boolean occluder) {
            return pass.sameOccluders(index, boxes, occluder);
        }

        public void beginChanges() {
            changes.clear(raster.tilesX(), raster.tilesY());
        }

        public void markChanged(int index) {
            if (!pass.occludes(index)) {
                return;
            }
            if (pass.sectionTiles(raster, index, range)) {
                changes.mark(range);
            } else {
                changes.markEverything();
            }
        }

        public boolean hiddenDespiteChanges(int index) {
            return !pass.isVisible(index) && pass.sectionTiles(raster, index, range) && !changes.any(range);
        }

        public int widen(float nextReach) {
            if (nextReach <= reach) {
                return 0;
            }
            int revealed = pass.widen(nextReach);
            reach = nextReach;
            return revealed;
        }

        public boolean isVisible(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
            return raster.isVisible(
                (float) (minX - eyeX) - reach, (float) (minY - eyeY) - reach, (float) (minZ - eyeZ) - reach,
                (float) (maxX - eyeX) + reach, (float) (maxY - eyeY) + reach, (float) (maxZ - eyeZ) + reach
            );
        }

        public DepthRaster raster() {
            return raster;
        }
    }
}
