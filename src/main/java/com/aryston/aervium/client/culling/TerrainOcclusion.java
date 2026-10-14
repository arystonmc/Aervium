package com.aryston.aervium.client.culling;

import com.aryston.aervium.Aervium;
import com.aryston.aervium.core.budget.SystemTimings;
import com.aryston.aervium.core.budget.TimedSystem;
import com.aryston.aervium.core.culling.ClipMatrix;
import com.aryston.aervium.core.culling.CullingStatistics;
import com.aryston.aervium.core.culling.DepthRaster;
import com.aryston.aervium.core.culling.OccluderBoxes;
import com.aryston.aervium.core.culling.OcclusionPass;
import com.aryston.aervium.core.culling.OcclusionPipeline;
import com.aryston.aervium.core.job.JobSystem;
import com.aryston.aervium.mixin.CompiledSectionMeshVisibilityAccessor;
import it.unimi.dsi.fastutil.HashCommon;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.renderer.chunk.CompiledSectionMesh;
import net.minecraft.client.renderer.chunk.SectionMesh;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
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
    private static final float FULLY_FADED_IN = 1.0F;
    private static final int MAX_SHADOW_CASCADES = 8;
    private static final long NO_SIGNATURE = 0L;

    private static final float GUARD_BAND = 0.8F;
    private static final float MIN_REACH = 0.125F;
    private static final float MAX_REACH = 4.0F;
    private static final float REACH_FRAMES = 3.0F;
    private static final long MAX_RESULT_AGE = 6L;

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
    private static final OcclusionPipeline[] SHADOW_CASCADES = shadowPipelines();
    private static final long[] SUBMITTED_SIGNATURES = new long[MAX_SHADOW_CASCADES];
    private static final long[] ACTIVE_SIGNATURES = new long[MAX_SHADOW_CASCADES];
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

    public static void setEnabled(boolean value) {
        enabled = value;
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
        Vec3 eye = camera.pos;
        OcclusionPipeline.Frame finished = CAMERA.activateFinished();
        if (finished != null) {
            markHidden(finished);
            CullingStatistics.SHARED.camera(finished.size(), finished.hidden());
            CullingStatistics.SHARED.cameraJob(finished.jobNanos());
        }
        OcclusionPipeline.Frame active = CAMERA.active();
        if (active != null && frame - active.frame() <= MAX_RESULT_AGE && active.covers(eye.x, eye.y, eye.z)) {
            current = active;
            currentEye = eye;
            if (CullingAudit.ENABLED && active == finished) {
                CullingAudit.afterCameraPass(active, eye);
            }
        }
        OcclusionPipeline.Frame next = CAMERA.begin(frame, eye.x, eye.y, eye.z, reach(eye));
        if (next != null) {
            snapshot(next, sections, camera, now, sectionFadeDuration);
            CAMERA.submit(next);
        }
        previousEye = eye;
        SystemTimings.SHARED.end(TimedSystem.CULLING, start);
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
        for (int index = 0, size = sections.size(); index < size; index++) {
            SectionRenderDispatcher.RenderSection section = sections.get(index);
            BlockPos origin = section.getRenderOrigin();
            next.add(
                section,
                origin.getX(),
                origin.getY(),
                origin.getZ(),
                occluders(section.getSectionMesh()),
                section.getVisibility(now, sectionFadeDuration) >= FULLY_FADED_IN
            );
        }
    }

    public static void filterShadowCasters(List<List<SectionRenderDispatcher.RenderSection>> cascades, Matrix4fc lightView, Vec3 camera, long now, long fadeDuration) {
        if (!enabled) {
            return;
        }
        long start = SystemTimings.begin();
        CullingStatistics.SHARED.beginShadows();
        for (int cascade = 0; cascade < cascades.size(); cascade++) {
            List<SectionRenderDispatcher.RenderSection> sections = cascades.get(cascade);
            int before = sections.size();
            int hidden = cascade < MAX_SHADOW_CASCADES ? filterCascade(cascade, sections, lightView, camera, now, fadeDuration) : 0;
            CullingStatistics.SHARED.shadowCascade(before, hidden);
        }
        CullingStatistics.SHARED.endShadows();
        SystemTimings.SHARED.end(TimedSystem.CULLING, start);
    }

    private static int filterCascade(int cascade, List<SectionRenderDispatcher.RenderSection> sections, Matrix4fc lightView, Vec3 camera, long now, long fadeDuration) {
        OcclusionPipeline pipeline = SHADOW_CASCADES[cascade];
        if (pipeline.activateFinished() != null) {
            ACTIVE_SIGNATURES[cascade] = SUBMITTED_SIGNATURES[cascade];
        }
        long signature = signature(sections, lightView, now, fadeDuration);
        OcclusionPipeline.Frame result = pipeline.active();
        if (result != null && ACTIVE_SIGNATURES[cascade] == signature && result.size() == sections.size()) {
            return removeHidden(sections, result);
        }
        if (sections.isEmpty() || SUBMITTED_SIGNATURES[cascade] == signature) {
            return 0;
        }
        OcclusionPipeline.Frame next = pipeline.begin(frame, camera.x, camera.y, camera.z, 0.0F);
        if (next != null) {
            snapshotShadow(next, sections, lightView, now, fadeDuration);
            SUBMITTED_SIGNATURES[cascade] = signature;
            pipeline.submit(next);
        }
        return 0;
    }

    private static int removeHidden(List<SectionRenderDispatcher.RenderSection> sections, OcclusionPipeline.Frame result) {
        int before = sections.size();
        int kept = 0;
        for (int index = 0; index < before; index++) {
            if (result.isVisible(index)) {
                sections.set(kept++, sections.get(index));
            }
        }
        sections.subList(kept, before).clear();
        return before - kept;
    }

    private static void snapshotShadow(OcclusionPipeline.Frame next, List<SectionRenderDispatcher.RenderSection> sections, Matrix4fc lightView, long now, long fadeDuration) {
        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        float maxZ = Float.NEGATIVE_INFINITY;
        for (SectionRenderDispatcher.RenderSection section : sections) {
            BlockPos origin = section.getRenderOrigin();
            float x = (float) (origin.getX() + HALF_SECTION - next.eyeX());
            float y = (float) (origin.getY() + HALF_SECTION - next.eyeY());
            float z = (float) (origin.getZ() + HALF_SECTION - next.eyeZ());
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
        for (int index = 0, size = sections.size(); index < size; index++) {
            SectionRenderDispatcher.RenderSection section = sections.get(index);
            BlockPos origin = section.getRenderOrigin();
            next.add(
                section,
                origin.getX(),
                origin.getY(),
                origin.getZ(),
                occluders(section.getSectionMesh()),
                section.getVisibility(now, fadeDuration) >= FULLY_FADED_IN
            );
        }
    }

    private static void clearShadowResults() {
        for (OcclusionPipeline pipeline : SHADOW_CASCADES) {
            pipeline.clear();
        }
        Arrays.fill(SUBMITTED_SIGNATURES, NO_SIGNATURE);
        Arrays.fill(ACTIVE_SIGNATURES, NO_SIGNATURE);
    }

    private static OcclusionPipeline[] shadowPipelines() {
        OcclusionPipeline[] pipelines = new OcclusionPipeline[MAX_SHADOW_CASCADES];
        for (int cascade = 0; cascade < pipelines.length; cascade++) {
            pipelines[cascade] = new OcclusionPipeline(SHADOW_JOBS::submit, MAX_SHADOW_OCCLUDER_BOXES);
        }
        return pipelines;
    }

    private static long signature(List<SectionRenderDispatcher.RenderSection> sections, Matrix4fc lightView, long now, long fadeDuration) {
        long hash = sections.size();
        hash = mix(hash, Float.floatToIntBits(lightView.m00()));
        hash = mix(hash, Float.floatToIntBits(lightView.m01()));
        hash = mix(hash, Float.floatToIntBits(lightView.m02()));
        hash = mix(hash, Float.floatToIntBits(lightView.m10()));
        hash = mix(hash, Float.floatToIntBits(lightView.m11()));
        hash = mix(hash, Float.floatToIntBits(lightView.m12()));
        for (int index = 0, size = sections.size(); index < size; index++) {
            SectionRenderDispatcher.RenderSection section = sections.get(index);
            hash = mix(hash, section.getSectionNode());
            hash = mix(hash, System.identityHashCode(occluders(section.getSectionMesh())));
            hash = mix(hash, section.getVisibility(now, fadeDuration) >= FULLY_FADED_IN ? 1 : 0);
        }
        return hash == 0L ? 1L : hash;
    }

    private static long mix(long hash, long value) {
        return HashCommon.mix(hash * 31 + value);
    }

    static OccluderBoxes occluders(SectionMesh mesh) {
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
