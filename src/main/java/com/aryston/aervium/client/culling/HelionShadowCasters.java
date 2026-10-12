package com.aryston.aervium.client.culling;

import com.aryston.aervium.Aervium;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Util;
import org.joml.Matrix4fc;
import org.jspecify.annotations.Nullable;

public final class HelionShadowCasters {
    private static @Nullable MethodHandle lightView;
    private static @Nullable Class<?> cascadeType;
    private static boolean failed;
    private static volatile boolean active;

    private HelionShadowCasters() {
    }

    public static boolean active() {
        return active;
    }

    public static void filter(List<?> cascades, @Nullable List<List<SectionRenderDispatcher.RenderSection>> sections) {
        if (failed || sections == null || cascades.isEmpty() || sections.size() != cascades.size() || !TerrainOcclusion.enabled()) {
            return;
        }
        Matrix4fc view = lightView(cascades.getFirst());
        if (view == null) {
            return;
        }
        active = true;
        CameraRenderState camera = Minecraft.getInstance().gameRenderer.gameRenderState().levelRenderState.cameraRenderState;
        TerrainOcclusion.filterShadowCasters(sections, view, camera.pos, Util.getMillis(), TerrainOcclusion.fadeDuration());
    }

    private static @Nullable Matrix4fc lightView(Object cascade) {
        try {
            if (lightView == null || cascadeType != cascade.getClass()) {
                cascadeType = cascade.getClass();
                lightView = MethodHandles.publicLookup().findVirtual(cascadeType, "lightView", MethodType.methodType(Matrix4fc.class));
            }
            return (Matrix4fc) lightView.invoke(cascade);
        } catch (Throwable failure) {
            failed = true;
            Aervium.LOGGER.warn("Aervium could not read the Helion shadow cascades; shadow caster culling stays off", failure);
            return null;
        }
    }
}
