package com.aryston.aervium.client.culling;

import com.aryston.aervium.core.budget.SystemTimings;
import com.aryston.aervium.core.budget.TimedSystem;
import com.aryston.aervium.core.culling.ClipMatrix;
import com.aryston.aervium.core.culling.CullingStatistics;
import com.aryston.aervium.core.culling.DepthRaster;
import com.aryston.aervium.core.culling.OccluderBoxes;
import com.aryston.aervium.core.culling.OcclusionPass;
import com.aryston.aervium.mixin.CompiledSectionMeshVisibilityAccessor;
import it.unimi.dsi.fastutil.HashCommon;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.renderer.chunk.CompiledSectionMesh;
import net.minecraft.client.renderer.chunk.SectionMesh;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
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
    private static final int MAX_CACHED_CASCADES = 8;

    private static final OcclusionPass PASS = new OcclusionPass();
    private static final DepthRaster SHADOW_RASTER = new DepthRaster(SHADOW_SIZE, SHADOW_SIZE);
    private static final ClipMatrix CLIP = new ClipMatrix();
    private static final ClipMatrix VIEW = new ClipMatrix();
    private static final float[] MATRIX = new float[16];
    private static final long[] CASCADE_SIGNATURES = new long[MAX_CACHED_CASCADES];
    private static final boolean[][] CASCADE_KEEP = new boolean[MAX_CACHED_CASCADES][];

    private static volatile boolean enabled = true;
    private static long fadeDuration = Long.MAX_VALUE;
    private static @Nullable DepthRaster cameraRaster;
    private static @Nullable CameraRenderState cameraPass;

    private TerrainOcclusion() {
    }

    public static boolean enabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
        Arrays.fill(CASCADE_SIGNATURES, 0L);
        if (!value) {
            CullingStatistics.SHARED.camera(0, 0);
            CullingStatistics.SHARED.beginShadows();
            CullingStatistics.SHARED.endShadows();
        }
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

    public static @Nullable OcclusionPass cameraVisibility(List<SectionRenderDispatcher.RenderSection> sections, long now, long fadeDuration) {
        CameraRenderState camera = cameraPass;
        if (camera == null) {
            return null;
        }
        long start = SystemTimings.begin();
        DepthRaster raster = cameraRaster(camera.projectionMatrix);
        camera.projectionMatrix.get(MATRIX);
        CLIP.set(MATRIX);
        camera.viewRotationMatrix.get(MATRIX);
        CLIP.multiply(VIEW.set(MATRIX)).depthFromW(CAMERA_NEAR, CAMERA_FAR);
        raster.begin(CLIP, true);
        fill(sections, camera.pos, now, fadeDuration);
        PASS.run(raster, MAX_CAMERA_OCCLUDER_BOXES);
        CullingStatistics.SHARED.camera(PASS.size(), PASS.hidden());
        if (CullingAudit.ENABLED) {
            CullingAudit.afterCameraPass(sections, PASS, camera.pos, CLIP);
        }
        SystemTimings.SHARED.end(TimedSystem.CULLING, start);
        return PASS;
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
            boolean[] keep = cascade < MAX_CACHED_CASCADES ? cachedKeep(cascade, sections, lightView, now, fadeDuration) : null;
            if (keep == null) {
                keep = computeShadowKeep(sections, lightView, camera, now, fadeDuration);
                if (cascade < MAX_CACHED_CASCADES) {
                    CASCADE_KEEP[cascade] = keep;
                }
            }
            int kept = 0;
            for (int index = 0; index < before; index++) {
                if (keep[index]) {
                    sections.set(kept++, sections.get(index));
                }
            }
            sections.subList(kept, before).clear();
            CullingStatistics.SHARED.shadowCascade(before, before - kept);
        }
        CullingStatistics.SHARED.endShadows();
        SystemTimings.SHARED.end(TimedSystem.CULLING, start);
    }

    private static boolean @Nullable [] cachedKeep(int cascade, List<SectionRenderDispatcher.RenderSection> sections, Matrix4fc lightView, long now, long fadeDuration) {
        long signature = signature(sections, lightView, now, fadeDuration);
        boolean[] keep = CASCADE_KEEP[cascade];
        boolean hit = CASCADE_SIGNATURES[cascade] == signature && keep != null && keep.length == sections.size();
        CASCADE_SIGNATURES[cascade] = signature;
        return hit ? keep : null;
    }

    private static boolean[] computeShadowKeep(List<SectionRenderDispatcher.RenderSection> sections, Matrix4fc lightView, Vec3 camera, long now, long fadeDuration) {
        boolean[] keep = new boolean[sections.size()];
        if (sections.isEmpty()) {
            return keep;
        }
        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        float maxZ = Float.NEGATIVE_INFINITY;
        for (SectionRenderDispatcher.RenderSection section : sections) {
            BlockPos origin = section.getRenderOrigin();
            float x = (float) (origin.getX() + HALF_SECTION - camera.x);
            float y = (float) (origin.getY() + HALF_SECTION - camera.y);
            float z = (float) (origin.getZ() + HALF_SECTION - camera.z);
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
        CLIP.orthographic(
            minX - SECTION_REACH, maxX + SECTION_REACH,
            minY - SECTION_REACH, maxY + SECTION_REACH,
            -(maxZ + SECTION_REACH), -(minZ - SECTION_REACH)
        );
        lightView.get(MATRIX);
        CLIP.multiply(VIEW.set(MATRIX));
        SHADOW_RASTER.begin(CLIP, false);
        fill(sections, camera, now, fadeDuration);
        PASS.run(SHADOW_RASTER, MAX_SHADOW_OCCLUDER_BOXES);
        for (int index = 0; index < keep.length; index++) {
            keep[index] = PASS.isVisible(index);
        }
        return keep;
    }

    private static void fill(List<SectionRenderDispatcher.RenderSection> sections, Vec3 camera, long now, long fadeDuration) {
        PASS.clear();
        for (int index = 0, size = sections.size(); index < size; index++) {
            SectionRenderDispatcher.RenderSection section = sections.get(index);
            BlockPos origin = section.getRenderOrigin();
            PASS.add(
                (float) (origin.getX() - camera.x),
                (float) (origin.getY() - camera.y),
                (float) (origin.getZ() - camera.z),
                occluders(section.getSectionMesh()),
                section.getVisibility(now, fadeDuration) >= FULLY_FADED_IN
            );
        }
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

    private static DepthRaster cameraRaster(Matrix4fc projection) {
        float aspect = Math.abs(projection.m11() / projection.m00());
        int height = Math.round(CAMERA_WIDTH / Math.max(aspect, 0.01F) / DepthRaster.TILE) * DepthRaster.TILE;
        height = Math.clamp(height, MIN_CAMERA_HEIGHT, MAX_CAMERA_HEIGHT);
        DepthRaster raster = cameraRaster;
        if (raster == null || raster.height() != height) {
            raster = new DepthRaster(CAMERA_WIDTH, height);
            cameraRaster = raster;
        }
        return raster;
    }
}
