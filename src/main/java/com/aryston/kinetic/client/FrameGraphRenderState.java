package com.aryston.kinetic.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2fc;
import org.jspecify.annotations.Nullable;

record FrameGraphRenderState(
    Matrix3x2fc pose,
    int[] bars,
    int count,
    @Nullable ScreenRectangle scissorArea,
    @Nullable ScreenRectangle bounds
) implements GuiElementRenderState {
    static final int INTS_PER_BAR = 4;
    private static final int TOP = 1;
    private static final int BOTTOM = 2;
    private static final int COLOR = 3;

    static FrameGraphRenderState of(Matrix3x2fc pose, int[] bars, int count, int left, int top, int width, int height, @Nullable ScreenRectangle scissorArea) {
        ScreenRectangle area = new ScreenRectangle(left, top, width, height).transformMaxBounds(pose);
        return new FrameGraphRenderState(pose, bars, count, scissorArea, scissorArea != null ? scissorArea.intersection(area) : area);
    }

    static void putBar(int[] bars, int bar, int x, int top, int bottom, int color) {
        int offset = bar * INTS_PER_BAR;
        bars[offset] = x;
        bars[offset + TOP] = top;
        bars[offset + BOTTOM] = bottom;
        bars[offset + COLOR] = color;
    }

    @Override
    public void buildVertices(VertexConsumer vertexConsumer) {
        for (int bar = 0; bar < count; bar++) {
            int offset = bar * INTS_PER_BAR;
            int x0 = bars[offset];
            int x1 = x0 + 1;
            int y0 = bars[offset + TOP];
            int y1 = bars[offset + BOTTOM];
            int color = bars[offset + COLOR];
            vertexConsumer.addVertexWith2DPose(pose, x0, y0).setColor(color);
            vertexConsumer.addVertexWith2DPose(pose, x0, y1).setColor(color);
            vertexConsumer.addVertexWith2DPose(pose, x1, y1).setColor(color);
            vertexConsumer.addVertexWith2DPose(pose, x1, y0).setColor(color);
        }
    }

    @Override
    public RenderPipeline pipeline() {
        return RenderPipelines.GUI;
    }

    @Override
    public TextureSetup textureSetup() {
        return TextureSetup.noTexture();
    }
}
