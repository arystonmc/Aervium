package com.aryston.kinetic.core.culling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class OcclusionPipelineTest {
    private static final OccluderBoxes SOLID = solid();
    private static final double EYE_X = 1000.5;
    private static final double EYE_Y = 70.25;
    private static final double EYE_Z = -300.75;

    @Test
    void resultArrivesOneFrameLaterAndNeverBlocks() {
        List<Runnable> queued = new ArrayList<>();
        OcclusionPipeline pipeline = new OcclusionPipeline(queued::add, 10_000);
        OcclusionPipeline.Frame first = pipeline.begin(0L, EYE_X, EYE_Y, EYE_Z, 0.125F);
        assertNotNull(first);
        Object target = fillWallScene(first);
        pipeline.submit(first);
        assertNull(pipeline.begin(1L, EYE_X, EYE_Y, EYE_Z, 0.125F));
        assertNull(pipeline.activateFinished());
        assertNull(pipeline.active());
        queued.removeFirst().run();
        OcclusionPipeline.Frame finished = pipeline.activateFinished();
        assertSame(first, finished);
        assertSame(first, pipeline.active());
        assertFalse(finished.isVisible(0));
        assertSame(target, finished.target(0));
        OcclusionPipeline.Frame second = pipeline.begin(2L, EYE_X, EYE_Y, EYE_Z, 0.125F);
        assertNotNull(second);
        assertNotSame(first, second);
        assertTrue(second.generation() > first.generation());
    }

    @Test
    void clearingDropsTheResultInFlight() {
        List<Runnable> queued = new ArrayList<>();
        OcclusionPipeline pipeline = new OcclusionPipeline(queued::add, 10_000);
        OcclusionPipeline.Frame frame = pipeline.begin(0L, EYE_X, EYE_Y, EYE_Z, 0.125F);
        fillWallScene(frame);
        pipeline.submit(frame);
        pipeline.clear();
        queued.removeFirst().run();
        assertNull(pipeline.activateFinished());
        assertNull(pipeline.active());
        assertNotNull(pipeline.begin(1L, EYE_X, EYE_Y, EYE_Z, 0.125F));
    }

    @Test
    void reachCoversOnlyNearbyEyes() {
        OcclusionPipeline pipeline = new OcclusionPipeline(Runnable::run, 10_000);
        OcclusionPipeline.Frame frame = pipeline.begin(0L, EYE_X, EYE_Y, EYE_Z, 0.5F);
        fillWallScene(frame);
        pipeline.submit(frame);
        assertSame(frame, pipeline.activateFinished());
        assertTrue(frame.covers(EYE_X + 0.3, EYE_Y, EYE_Z + 0.3));
        assertFalse(frame.covers(EYE_X + 0.6, EYE_Y, EYE_Z));
        assertFalse(frame.isVisible(EYE_X - 2.0, EYE_Y - 2.0, EYE_Z - 100.0, EYE_X + 2.0, EYE_Y + 2.0, EYE_Z - 96.0));
        assertTrue(frame.isVisible(EYE_X - 2.0, EYE_Y - 2.0, EYE_Z - 4.0, EYE_X + 2.0, EYE_Y + 2.0, EYE_Z - 3.0));
    }

    @Test
    void matchesTheSynchronousPass() {
        OcclusionPipeline pipeline = new OcclusionPipeline(Runnable::run, 10_000);
        OcclusionPipeline.Frame frame = pipeline.begin(0L, EYE_X, EYE_Y, EYE_Z, 0.0F);
        fillWallScene(frame);
        pipeline.submit(frame);
        pipeline.activateFinished();

        DepthRaster raster = new DepthRaster(128, 64);
        raster.begin(view(), false);
        OcclusionPass pass = new OcclusionPass();
        pass.add(-8, -8, -96, OccluderBoxes.NONE, false);
        for (int x = -10; x < 10; x++) {
            for (int y = -10; y < 10; y++) {
                pass.add(x * 16, y * 16, -48, SOLID, true);
            }
        }
        pass.run(raster, 10_000);
        assertEquals(pass.size(), frame.size());
        assertEquals(pass.hidden(), frame.hidden());
        for (int index = 0; index < pass.size(); index++) {
            assertEquals(pass.isVisible(index), frame.isVisible(index));
        }
    }

    @Test
    void wideningReachesFartherEyesAndOnlyRevealsSections() {
        OcclusionPipeline pipeline = new OcclusionPipeline(Runnable::run, 10_000);
        OcclusionPipeline.Frame frame = pipeline.begin(0L, EYE_X, EYE_Y, EYE_Z, 0.125F);
        fillWallScene(frame);
        pipeline.submit(frame);
        pipeline.activateFinished();
        int hiddenBefore = frame.hidden();
        assertFalse(frame.isVisible(0));
        assertFalse(frame.covers(EYE_X + 2.0, EYE_Y, EYE_Z));

        assertEquals(0, frame.widen(0.1F));
        assertEquals(0, frame.widen(2.5F));
        assertTrue(frame.covers(EYE_X + 2.0, EYE_Y, EYE_Z));
        assertFalse(frame.isVisible(0));
        assertEquals(hiddenBefore, frame.hidden());

        assertEquals(1, frame.widen(200.0F));
        assertTrue(frame.isVisible(0));
        assertEquals(hiddenBefore - 1, frame.hidden());
    }

    @Test
    void aChangedOccluderInFrontRevealsOnlyTheSectionsBehindIt() {
        OcclusionPipeline pipeline = new OcclusionPipeline(Runnable::run, 10_000);
        OcclusionPipeline.Frame frame = pipeline.begin(0L, EYE_X, EYE_Y, EYE_Z, 0.0F);
        fillWallScene(frame);
        pipeline.submit(frame);
        pipeline.activateFinished();
        int wallInFront = wallIndex(0, 0);
        int wallFarAway = wallIndex(-10, -10);

        frame.beginChanges();
        assertTrue(frame.hiddenDespiteChanges(0));
        assertFalse(frame.hiddenDespiteChanges(wallInFront));

        frame.markChanged(wallFarAway);
        assertTrue(frame.hiddenDespiteChanges(0));

        frame.markChanged(wallInFront);
        assertFalse(frame.hiddenDespiteChanges(0));

        frame.beginChanges();
        assertTrue(frame.hiddenDespiteChanges(0));
    }

    @Test
    void onlyTheSameBoxesAndFadeStateCountAsUnchanged() {
        OcclusionPipeline pipeline = new OcclusionPipeline(Runnable::run, 10_000);
        OcclusionPipeline.Frame frame = pipeline.begin(0L, EYE_X, EYE_Y, EYE_Z, 0.0F);
        fillWallScene(frame);
        pipeline.submit(frame);
        pipeline.activateFinished();
        int wall = wallIndex(0, 0);

        assertTrue(frame.sameOccluders(wall, SOLID, true));
        assertFalse(frame.sameOccluders(wall, SOLID, false));
        assertFalse(frame.sameOccluders(wall, solid(), true));
        assertTrue(frame.sameOccluders(0, OccluderBoxes.NONE, true));
    }

    private static int wallIndex(int x, int y) {
        return 1 + (x + 10) * 20 + (y + 10);
    }

    private static Object fillWallScene(OcclusionPipeline.Frame frame) {
        frame.raster(128, 64);
        frame.clip().copy(view());
        Object target = new Object();
        frame.add(target, EYE_X - 8, EYE_Y - 8, EYE_Z - 96, OccluderBoxes.NONE, false);
        for (int x = -10; x < 10; x++) {
            for (int y = -10; y < 10; y++) {
                frame.add(new Object(), EYE_X + x * 16, EYE_Y + y * 16, EYE_Z - 48, SOLID, true);
            }
        }
        return target;
    }

    private static ClipMatrix view() {
        return new ClipMatrix().perspective((float) Math.toRadians(70.0), 2.0F, 0.5F, 512.0F);
    }

    private static OccluderBoxes solid() {
        OpaqueMask mask = new OpaqueMask();
        for (int y = 0; y < 16; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    mask.set(x, y, z);
                }
            }
        }
        return mask.boxes();
    }
}
