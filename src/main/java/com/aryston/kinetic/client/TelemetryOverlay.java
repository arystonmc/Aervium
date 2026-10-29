package com.aryston.kinetic.client;

import com.aryston.kinetic.core.budget.FrameBudget;
import com.aryston.kinetic.core.budget.SystemTimings;
import com.aryston.kinetic.core.budget.TimedSystem;
import com.aryston.kinetic.core.telemetry.TelemetrySnapshot;
import java.util.List;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.client.gui.GuiLayer;
import org.joml.Matrix3x2f;

final class TelemetryOverlay implements GuiLayer {
    private static final int GRAPH_WIDTH = 240;
    private static final int GRAPH_HEIGHT = 60;
    private static final long GRAPH_TOP_NANOS = 50_000_000L;
    private static final int MARGIN = 4;
    private static final int PADDING = 4;
    private static final int LINE_HEIGHT = 10;
    private static final int GAP = 4;
    private static final int BACKGROUND = 0xA0101418;
    private static final int GRAPH_BACKGROUND = 0x60000000;
    private static final int BAR_SMOOTH = 0xFF4CD08A;
    private static final int BAR_SLOW = 0xFFFFC857;
    private static final int BAR_SPIKE = 0xFFFF5C5C;
    private static final int BUDGET_LINE = 0xC0FFFFFF;
    private static final int SPIKE_FACTOR = 2;

    private final ClientTelemetry telemetry;
    private final long[] graph = new long[GRAPH_WIDTH];
    private List<OverlayLines.Line> lines = List.of(new OverlayLines.Line("Kinetic  collecting...", OverlayLines.TEXT));
    private List<FormattedCharSequence> visualLines = List.of();
    private int textWidth;
    private boolean visible;
    private FrameBudget budget;

    TelemetryOverlay(ClientTelemetry telemetry, boolean visible, FrameBudget budget) {
        this.telemetry = telemetry;
        this.visible = visible;
        this.budget = budget;
    }

    boolean isVisible() {
        return visible;
    }

    void setVisible(boolean visible) {
        this.visible = visible;
    }

    void setBudget(FrameBudget budget) {
        this.budget = budget;
    }

    FrameBudget budget() {
        return budget;
    }

    void publish(TelemetrySnapshot snapshot, List<String> passiveRendererMods) {
        lines = OverlayLines.of(snapshot, passiveRendererMods);
        visualLines = visualOrder(lines);
        Font font = Minecraft.getInstance().font;
        textWidth = lines.stream().mapToInt(line -> font.width(line.text())).max().orElse(0);
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        if (!visible) {
            return;
        }
        long start = SystemTimings.begin();
        int width = Math.max(GRAPH_WIDTH, textWidth) + PADDING * 2;
        int height = PADDING * 2 + lines.size() * LINE_HEIGHT + GAP + GRAPH_HEIGHT;
        int left = graphics.guiWidth() - width - MARGIN;
        int top = MARGIN;
        graphics.fill(left, top, left + width, top + height, BACKGROUND);
        drawLines(graphics, left + PADDING, top + PADDING);
        drawGraph(graphics, left + PADDING, top + PADDING + lines.size() * LINE_HEIGHT + GAP);
        SystemTimings.SHARED.end(TimedSystem.OVERLAY, start);
    }

    private void drawLines(GuiGraphicsExtractor graphics, int x, int y) {
        Font font = Minecraft.getInstance().font;
        if (visualLines.size() != lines.size()) {
            visualLines = visualOrder(lines);
        }
        for (int i = 0; i < lines.size(); i++) {
            graphics.text(font, visualLines.get(i), x, y + i * LINE_HEIGHT, lines.get(i).color());
        }
    }

    private static List<FormattedCharSequence> visualOrder(List<OverlayLines.Line> lines) {
        Language language = Language.getInstance();
        return lines.stream().map(line -> language.getVisualOrder(FormattedText.of(line.text()))).toList();
    }

    private void drawGraph(GuiGraphicsExtractor graphics, int left, int top) {
        int bottom = top + GRAPH_HEIGHT;
        graphics.fill(left, top, left + GRAPH_WIDTH, bottom, GRAPH_BACKGROUND);
        long frameBudget = budget.frameNanos();
        int count = telemetry.frameTimes().copyLatest(graph, GRAPH_WIDTH);
        int firstX = left + GRAPH_WIDTH - count;
        int[] bars = new int[count * FrameGraphRenderState.INTS_PER_BAR];
        for (int i = 0; i < count; i++) {
            long nanos = graph[i];
            FrameGraphRenderState.putBar(bars, i, firstX + i, bottom - Math.max(1, scaled(nanos)), bottom, barColor(nanos, frameBudget));
        }
        graphics.submitGuiElementRenderState(FrameGraphRenderState.of(
            new Matrix3x2f(graphics.pose()), bars, count, left, top, GRAPH_WIDTH, GRAPH_HEIGHT, graphics.peekScissorStack()
        ));
        int budgetY = bottom - scaled(frameBudget);
        graphics.fill(left, budgetY, left + GRAPH_WIDTH, budgetY + 1, BUDGET_LINE);
    }

    private static int scaled(long nanos) {
        return (int) Math.min(GRAPH_HEIGHT, nanos * GRAPH_HEIGHT / GRAPH_TOP_NANOS);
    }

    private static int barColor(long nanos, long frameBudget) {
        if (nanos > frameBudget * SPIKE_FACTOR) {
            return BAR_SPIKE;
        }
        return nanos > frameBudget ? BAR_SLOW : BAR_SMOOTH;
    }
}
