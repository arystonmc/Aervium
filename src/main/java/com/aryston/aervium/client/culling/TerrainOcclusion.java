package com.aryston.aervium.client.culling;

import com.aryston.aervium.Aervium;
import com.aryston.aervium.client.FrameTrace;
import com.aryston.aervium.core.budget.SystemTimings;
import com.aryston.aervium.core.budget.TimedSystem;
import com.aryston.aervium.core.culling.CameraResult;
import com.aryston.aervium.core.culling.ClipMatrix;
import com.aryston.aervium.core.culling.CullingStatistics;
import com.aryston.aervium.core.culling.CullingYield;
import com.aryston.aervium.core.culling.DepthRaster;
import com.aryston.aervium.core.culling.OccluderBoxes;
import com.aryston.aervium.core.culling.OcclusionPass;
import com.aryston.aervium.core.culling.OcclusionPipeline;
import com.aryston.aervium.core.job.JobSystem;
import com.aryston.aervium.client.chunk.MeshVersionHolder;
import com.aryston.aervium.mixin.CompiledSectionMeshVisibilityAccessor;
import java.util.List;
import net.minecraft.client.renderer.chunk.CompiledSectionMesh;
import net.minecraft.client.renderer.chunk.SectionMesh;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.SectionPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;
import org.jspecify.annotations.Nullable;

public final class TerrainOcclusion {
    private static final int CAMERA_WIDTH = 256;
    private static final int MIN_CAMERA_HEIGHT = 64;
    private static final int MAX_CAMERA_HEIGHT = 256;
    private static final int SHADOW_SIZE = 256;
    private static final int MAX_CAMERA_OCCLUDER_BOXES = 4096;
    private static final int MAX_SHADOW_OCCLUDER_BOXES = 16384;
    private static final float CAMERA_NEAR = 0.5F;
    private static final float CAMERA_FAR = 4096.0F;
    private static final float SECTION_REACH = OcclusionPass.SECTION_SIZE * 0.5F * (float) Math.sqrt(3.0) + OcclusionPass.GEOMETRY_MARGIN;
    private static final float HALF_SECTION = OcclusionPass.SECTION_SIZE * 0.5F;
    static final float FULLY_FADED_IN = 1.0F;
    private static final int MAX_SHADOW_CASCADES = 8;

    private static final float GUARD_BAND = 0.8F;
    private static final float MIN_REACH = 0.125F;
    private static final float MAX_REACH = 4.0F;
    private static final float REACH_FRAMES = 3.0F;
    private static final long MAX_RESULT_AGE = 6L;
    private static final long MAX_RESULT_AGE_NANOS = 60_000_000L;
    private static final float MAX_WIDENED_REACH = OcclusionPass.MAX_REVEAL_REACH;
    private static final int NOT_HIDDEN = 0;
    private static final float USEFUL_HIDDEN_SHARE = 0.15F;
    private static final int LOW_YIELD_RESULTS = 20;
    private static final long YIELD_PAUSE_NANOS = 1_000_000_000L;
    private static final CullingYield CAMERA_YIELD = new CullingYield(USEFUL_HIDDEN_SHARE, LOW_YIELD_RESULTS, YIELD_PAUSE_NANOS);
    private static final CullingYield SHADOW_YIELD = new CullingYield(USEFUL_HIDDEN_SHARE, LOW_YIELD_RESULTS, YIELD_PAUSE_NANOS);

    private static final JobSystem JOBS = new JobSystem(
        "Aervium Culling",
        1,
        Thread.NORM_PRIORITY,
        (thread, failure) -> Aervium.LOGGER.error("Aervium culling worker {} failed", thread.getName(), failure)
    );
    private static final OcclusionPipeline CAMERA = new OcclusionPipeline(JOBS::submit, MAX_CAMERA_OCCLUDER_BOXES);
    private static final JobSystem SHADOW_JOBS = new JobSystem(
        "Aervium Shadow Culling",
        1,
        Thread.NORM_PRIORITY,
        (thread, failure) -> Aervium.LOGGER.error("Aervium shadow culling worker {} failed", thread.getName(), failure)
    );
    private static final ShadowCascadeCulling[] SHADOW_CASCADES = shadowCascades();
    private static final ClipMatrix VIEW = new ClipMatrix();
    private static final float[] MATRIX = new float[16];

    private static volatile boolean enabled = true;
    private static long fadeDuration = Long.MAX_VALUE;
    private static @Nullable CameraRenderState cameraPass;
    private static OcclusionPipeline.@Nullable Frame current;
    private static @Nullable Vec3 currentEye;
    private static @Nullable Vec3 previousEye;
    private static long frame;

    private TerrainOcclusion() {
    }

    public static boolean enabled() {
        return enabled;
    }

    public static boolean cameraPaused() {
        return enabled && CAMERA_YIELD.paused(System.nanoTime());
    }

    public static boolean shadowsPaused() {
        return enabled && SHADOW_YIELD.paused(System.nanoTime());
    }

    public static void setEnabled(boolean value) {
        enabled = value;
        CAMERA_YIELD.reset();
        SHADOW_YIELD.reset();
        clearShadowResults();
        if (!value) {
            CullingStatistics.SHARED.camera(0, 0);
            CullingStatistics.SHARED.beginShadows();
            CullingStatistics.SHARED.endShadows();
        }
    }

    public static void frameFinished() {
        frame++;
        current = null;
    }

    public static long fadeDuration() {
        return fadeDuration;
    }

    public static void beginTerrainPass(Matrix4fc modelView, CameraRenderState camera, long sectionFadeDuration) {
        fadeDuration = sectionFadeDuration;
        boolean usable = enabled && camera.initialized && !camera.isPanoramicMode && !camera.isFrustumCaptured;
        cameraPass = usable && modelView.equals(camera.viewRotationMatrix) ? camera : null;
    }

    public static void endTerrainPass() {
        cameraPass = null;
    }

    public static boolean cullsCameraSections() {
        return cameraPass != null && current != null;
    }

    public static boolean hiddenFromCamera(SectionRenderDispatcher.RenderSection section) {
        OcclusionPipeline.Frame result = current;
        return result != null && ((CameraOcclusionHolder) section).aervium$hiddenGeneration() == result.generation();
    }

    public static void prepareCamera(List<SectionRenderDispatcher.RenderSection> sections, CameraRenderState camera, long now, long sectionFadeDuration) {
        current = null;
        CullingStatistics.SHARED.entities(EntityOcclusion.takeTested(), EntityOcclusion.takeHidden());
        CullingStatistics.SHARED.particles(ParticleOcclusion.takeTested(), ParticleOcclusion.takeHidden());
        if (!enabled || !camera.initialized || camera.isPanoramicMode || camera.isFrustumCaptured) {
            CAMERA.clear();
            previousEye = null;
            return;
        }
        long start = SystemTimings.begin();
        long nanos = System.nanoTime();
        if (CAMERA_YIELD.paused(nanos)) {
            SystemTimings.SHARED.end(TimedSystem.CULLING, start);
            return;
        }
        Vec3 eye = camera.pos;
        OcclusionPipeline.Frame finished = CAMERA.activateFinished();
        if (finished != null) {
            CullingStatistics.SHARED.camera(finished.size(), finished.hidden());
            CullingStatistics.SHARED.cameraJob(finished.jobNanos());
            if (CAMERA_YIELD.result(finished.size(), finished.hidden(), nanos)) {
                CAMERA.clear();
                previousEye = null;
                SystemTimings.SHARED.end(TimedSystem.CULLING, start);
                return;
            }
            markHidden(finished);
        }
        OcclusionPipeline.Frame active = CAMERA.active();
        float reach = reach(eye);
        CameraResult result = cameraResult(active, eye, reach);
        CullingStatistics.SHARED.cameraResult(result);
        FrameTrace.cameraResult(result, sections.size(), active == null ? 0 : active.hidden());
        if (result == CameraResult.CURRENT || result == CameraResult.WIDENED) {
            current = active;
            currentEye = eye;
            if (CullingAudit.ENABLED && (active == finished || result == CameraResult.WIDENED)) {
                CullingAudit.afterCameraPass(active, eye);
            }
        }
        OcclusionPipeline.Frame next = CAMERA.begin(frame, eye.x, eye.y, eye.z, reach);
        if (next != null) {
            snapshot(next, sections, camera, now, sectionFadeDuration);
            CAMERA.submit(next);
        }
        previousEye = eye;
        SystemTimings.SHARED.end(TimedSystem.CULLING, start);
        FrameTrace.culled(System.nanoTime() - nanos);
    }

    public static boolean isVisibleToCamera(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        OcclusionPipeline.Frame result = current;
        if (result == null) {
            return true;
        }
        if (!Double.isFinite(minX) || !Double.isFinite(minY) || !Double.isFinite(minZ)
            || !Double.isFinite(maxX) || !Double.isFinite(maxY) || !Double.isFinite(maxZ)) {
            return true;
        }
        return result.isVisible(minX, minY, minZ, maxX, maxY, maxZ);
    }

    static void auditHiddenBox(AABB box) {
        OcclusionPipeline.Frame result = current;
        Vec3 eye = currentEye;
        if (result != null && eye != null) {
            CullingAudit.hiddenBox(box, eye, result.clip());
        }
    }

    private static CameraResult cameraResult(OcclusionPipeline.@Nullable Frame active, Vec3 eye, float reach) {
        if (active == null) {
            return CameraResult.NONE;
        }
        if (frame - active.frame() > MAX_RESULT_AGE && System.nanoTime() - active.submittedNanos() > MAX_RESULT_AGE_NANOS) {
            return CameraResult.TOO_OLD;
        }
        if (active.covers(eye.x, eye.y, eye.z)) {
            return CameraResult.CURRENT;
        }
        float widened = (float) active.distanceTo(eye.x, eye.y, eye.z) + reach;
        if (widened > MAX_WIDENED_REACH) {
            return CameraResult.OUT_OF_REACH;
        }
        unmarkRevealed(active, active.widen(widened));
        return CameraResult.WIDENED;
    }

    private static void unmarkRevealed(OcclusionPipeline.Frame result, int revealed) {
        int generation = result.generation();
        for (int rank = 0; rank < revealed; rank++) {
            CameraOcclusionHolder section = (CameraOcclusionHolder) result.target(result.revealed(rank));
            if (section.aervium$hiddenGeneration() == generation) {
                section.aervium$setHiddenGeneration(NOT_HIDDEN);
            }
        }
    }

    private static float reach(Vec3 eye) {
        Vec3 previous = previousEye;
        float moved = previous == null ? MAX_REACH : (float) eye.distanceTo(previous);
        return Math.clamp(MIN_REACH + moved * REACH_FRAMES, MIN_REACH, MAX_REACH);
    }

    private static void markHidden(OcclusionPipeline.Frame result) {
        int generation = result.generation();
        for (int index = 0, size = result.size(); index < size; index++) {
            if (!result.isVisible(index)) {
                ((CameraOcclusionHolder) result.target(index)).aervium$setHiddenGeneration(generation);
            }
        }
    }

    private static void snapshot(OcclusionPipeline.Frame next, List<SectionRenderDispatcher.RenderSection> sections, CameraRenderState camera, long now, long sectionFadeDuration) {
        next.raster(CAMERA_WIDTH, cameraHeight(camera.projectionMatrix));
        camera.projectionMatrix.get(MATRIX);
        for (int column = 0; column < 4; column++) {
            MATRIX[column * 4] *= GUARD_BAND;
            MATRIX[column * 4 + 1] *= GUARD_BAND;
        }
        next.clip().set(MATRIX);
        camera.viewRotationMatrix.get(MATRIX);
        next.clip().multiply(VIEW.set(MATRIX)).depthFromW(CAMERA_NEAR, CAMERA_FAR);
        addSections(next, sections, now, sectionFadeDuration);
    }

    public static void filterShadowCasters(List<List<SectionRenderDispatcher.RenderSection>> cascades, Matrix4fc lightView, Vec3 camera, long now, long fadeDuration) {
        if (!enabled) {
            return;
        }
        long start = SystemTimings.begin();
        long nanos = System.nanoTime();
        if (SHADOW_YIELD.paused(nanos)) {
            SystemTimings.SHARED.end(TimedSystem.CULLING, start);
            return;
        }
        CullingStatistics.SHARED.beginShadows();
        int tested = 0;
        int hiddenTotal = 0;
        for (int cascade = 0; cascade < cascades.size(); cascade++) {
            List<SectionRenderDispatcher.RenderSection> sections = cascades.get(cascade);
            int before = sections.size();
            int hidden = cascade < MAX_SHADOW_CASCADES ? SHADOW_CASCADES[cascade].filter(sections, lightView, camera, frame, now, fadeDuration) : 0;
            CullingStatistics.SHARED.shadowCascade(before, hidden);
            tested += before;
            hiddenTotal += hidden;
        }
        CullingStatistics.SHARED.endShadows();
        if (SHADOW_YIELD.result(tested, hiddenTotal, nanos)) {
            clearShadowResults();
        }
        SystemTimings.SHARED.end(TimedSystem.CULLING, start);
    }

    static void snapshotShadow(OcclusionPipeline.Frame next, List<SectionRenderDispatcher.RenderSection> sections, Matrix4fc lightView, long now, long fadeDuration) {
        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        float maxZ = Float.NEGATIVE_INFINITY;
        for (SectionRenderDispatcher.RenderSection section : sections) {
            long node = section.getSectionNode();
            float x = (float) (originX(node) + HALF_SECTION - next.eyeX());
            float y = (float) (originY(node) + HALF_SECTION - next.eyeY());
            float z = (float) (originZ(node) + HALF_SECTION - next.eyeZ());
            float lightX = lightView.m00() * x + lightView.m10() * y + lightView.m20() * z + lightView.m30();
            float lightY = lightView.m01() * x + lightView.m11() * y + lightView.m21() * z + lightView.m31();
            float lightZ = lightView.m02() * x + lightView.m12() * y + lightView.m22() * z + lightView.m32();
            minX = Math.min(minX, lightX);
            minY = Math.min(minY, lightY);
            minZ = Math.min(minZ, lightZ);
            maxX = Math.max(maxX, lightX);
            maxY = Math.max(maxY, lightY);
            maxZ = Math.max(maxZ, lightZ);
        }
        next.raster(SHADOW_SIZE, SHADOW_SIZE);
        next.clip().orthographic(
            minX - SECTION_REACH, maxX + SECTION_REACH,
            minY - SECTION_REACH, maxY + SECTION_REACH,
            -(maxZ + SECTION_REACH), -(minZ - SECTION_REACH)
        );
        lightView.get(MATRIX);
        next.clip().multiply(VIEW.set(MATRIX));
        addSections(next, sections, now, fadeDuration);
    }

    private static void addSections(OcclusionPipeline.Frame next, List<SectionRenderDispatcher.RenderSection> sections, long now, long sectionFadeDuration) {
        for (int index = 0, size = sections.size(); index < size; index++) {
            SectionRenderDispatcher.RenderSection section = sections.get(index);
            long node = section.getSectionNode();
            next.add(
                section,
                originX(node),
                originY(node),
                originZ(node),
                occluders(section),
                section.getVisibility(now, sectionFadeDuration) >= FULLY_FADED_IN
            );
        }
    }

    private static int originX(long sectionNode) {
        return SectionPos.sectionToBlockCoord(SectionPos.x(sectionNode));
    }

    private static int originY(long sectionNode) {
        return SectionPos.sectionToBlockCoord(SectionPos.y(sectionNode));
    }

    private static int originZ(long sectionNode) {
        return SectionPos.sectionToBlockCoord(SectionPos.z(sectionNode));
    }

    private static void clearShadowResults() {
        for (ShadowCascadeCulling cascade : SHADOW_CASCADES) {
            cascade.clear();
        }
    }

    private static ShadowCascadeCulling[] shadowCascades() {
        ShadowCascadeCulling[] cascades = new ShadowCascadeCulling[MAX_SHADOW_CASCADES];
        for (int cascade = 0; cascade < cascades.length; cascade++) {
            cascades[cascade] = new ShadowCascadeCulling(SHADOW_JOBS::submit, MAX_SHADOW_OCCLUDER_BOXES);
        }
        return cascades;
    }

    static OccluderBoxes occluders(SectionRenderDispatcher.RenderSection section) {
        int meshVersion = ((MeshVersionHolder) section).aervium$meshVersion();
        CameraOcclusionHolder holder = (CameraOcclusionHolder) section;
        OccluderBoxes cached = holder.aervium$occludersFor(meshVersion);
        if (cached != null) {
            return cached;
        }
        OccluderBoxes boxes = occluders(section.getSectionMesh());
        holder.aervium$cacheOccluders(boxes, meshVersion);
        return boxes;
    }

    private static OccluderBoxes occluders(SectionMesh mesh) {
        if (mesh instanceof CompiledSectionMesh compiled) {
            return ((OccluderHolder) ((CompiledSectionMeshVisibilityAccessor) compiled).aervium$visibilitySet()).aervium$occluders();
        }
        return OccluderBoxes.NONE;
    }

    private static int cameraHeight(Matrix4fc projection) {
        float aspect = Math.abs(projection.m11() / projection.m00());
        int height = Math.round(CAMERA_WIDTH / Math.max(aspect, 0.01F) / DepthRaster.TILE) * DepthRaster.TILE;
        return Math.clamp(height, MIN_CAMERA_HEIGHT, MAX_CAMERA_HEIGHT);
    }
}
