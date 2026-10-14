package com.aryston.aervium.client.culling;

import com.aryston.aervium.core.culling.OccluderBoxes;
import com.aryston.aervium.core.culling.OcclusionPipeline;
import it.unimi.dsi.fastutil.HashCommon;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;
import org.jspecify.annotations.Nullable;

final class ShadowCascadeCulling {
    private static final int NOT_FOUND = -1;
    private static final long NO_SIGNATURE = 0L;
    private static final int INITIAL_CAPACITY = 1024;

    private final OcclusionPipeline pipeline;
    private final Long2IntOpenHashMap indexBySection = new Long2IntOpenHashMap();
    private boolean[] present = new boolean[INITIAL_CAPACITY];
    private int[] matched = new int[INITIAL_CAPACITY];
    private long submittedSignature = NO_SIGNATURE;
    private long submittedLight = NO_SIGNATURE;
    private long activeLight = NO_SIGNATURE;

    ShadowCascadeCulling(Executor executor, int maxOccluderBoxes) {
        this.pipeline = new OcclusionPipeline(executor, maxOccluderBoxes);
        indexBySection.defaultReturnValue(NOT_FOUND);
    }

    int filter(List<SectionRenderDispatcher.RenderSection> sections, Matrix4fc lightView, Vec3 camera, long frame, long now, long fadeDuration) {
        OcclusionPipeline.Frame finished = pipeline.activateFinished();
        if (finished != null) {
            activeLight = submittedLight;
            index(finished);
        }
        long light = lightSignature(lightView);
        OcclusionPipeline.Frame result = pipeline.active();
        boolean usable = result != null && activeLight == light;
        long signature = match(sections, light, usable ? result : null, now, fadeDuration);
        if (!sections.isEmpty() && signature != submittedSignature) {
            OcclusionPipeline.Frame next = pipeline.begin(frame, camera.x, camera.y, camera.z, 0.0F);
            if (next != null) {
                TerrainOcclusion.snapshotShadow(next, sections, lightView, now, fadeDuration);
                submittedSignature = signature;
                submittedLight = light;
                pipeline.submit(next);
            }
        }
        return usable ? removeHidden(sections, result) : 0;
    }

    void clear() {
        pipeline.clear();
        indexBySection.clear();
        submittedSignature = NO_SIGNATURE;
        submittedLight = NO_SIGNATURE;
        activeLight = NO_SIGNATURE;
    }

    private void index(OcclusionPipeline.Frame result) {
        indexBySection.clear();
        for (int index = 0, size = result.size(); index < size; index++) {
            indexBySection.put(((SectionRenderDispatcher.RenderSection) result.target(index)).getSectionNode(), index);
        }
    }

    private long match(List<SectionRenderDispatcher.RenderSection> sections, long light, OcclusionPipeline.@Nullable Frame result, long now, long fadeDuration) {
        int size = sections.size();
        if (matched.length < size) {
            matched = new int[Math.max(size, matched.length * 2)];
        }
        if (result != null) {
            if (present.length < result.size()) {
                present = new boolean[Math.max(result.size(), present.length * 2)];
            }
            Arrays.fill(present, 0, result.size(), false);
        }
        long hash = mix(light, size);
        for (int position = 0; position < size; position++) {
            SectionRenderDispatcher.RenderSection section = sections.get(position);
            OccluderBoxes boxes = TerrainOcclusion.occluders(section.getSectionMesh());
            boolean occluder = section.getVisibility(now, fadeDuration) >= TerrainOcclusion.FULLY_FADED_IN;
            long node = section.getSectionNode();
            hash = mix(mix(mix(hash, node), System.identityHashCode(boxes)), occluder ? 1 : 0);
            matched[position] = result == null ? NOT_FOUND : find(result, node, boxes, occluder);
        }
        return hash == NO_SIGNATURE ? 1L : hash;
    }

    private int find(OcclusionPipeline.Frame result, long node, OccluderBoxes boxes, boolean occluder) {
        int index = indexBySection.get(node);
        if (index == NOT_FOUND || !result.sameOccluders(index, boxes, occluder)) {
            return NOT_FOUND;
        }
        present[index] = true;
        return index;
    }

    private int removeHidden(List<SectionRenderDispatcher.RenderSection> sections, OcclusionPipeline.Frame result) {
        result.beginChanges();
        for (int index = 0, size = result.size(); index < size; index++) {
            if (!present[index]) {
                result.markChanged(index);
            }
        }
        int before = sections.size();
        int kept = 0;
        for (int position = 0; position < before; position++) {
            int index = matched[position];
            if (index == NOT_FOUND || !result.hiddenDespiteChanges(index)) {
                sections.set(kept++, sections.get(position));
            }
        }
        sections.subList(kept, before).clear();
        return before - kept;
    }

    private static long lightSignature(Matrix4fc lightView) {
        long hash = mix(1L, Float.floatToIntBits(lightView.m00()));
        hash = mix(hash, Float.floatToIntBits(lightView.m01()));
        hash = mix(hash, Float.floatToIntBits(lightView.m02()));
        hash = mix(hash, Float.floatToIntBits(lightView.m10()));
        hash = mix(hash, Float.floatToIntBits(lightView.m11()));
        hash = mix(hash, Float.floatToIntBits(lightView.m12()));
        return hash == NO_SIGNATURE ? 1L : hash;
    }

    private static long mix(long hash, long value) {
        return HashCommon.mix(hash * 31 + value);
    }
}
