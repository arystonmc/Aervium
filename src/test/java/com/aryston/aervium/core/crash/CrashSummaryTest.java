package com.aryston.aervium.core.crash;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CrashSummaryTest {
    private static final Map<String, Boolean> MODULES = new LinkedHashMap<>();

    static {
        MODULES.put("occlusionCulling", true);
        MODULES.put("meshWorkers", false);
    }

    @Test
    void reportsNoAerviumCodeWhenTheStackHasNone() {
        CrashSummary.Involvement involvement = CrashSummary.find(List.of(
            new CrashSummary.Frame("net.minecraft.client.Minecraft", "run"),
            new CrashSummary.Frame("java.lang.Thread", "run")), (type, handler) -> Set.of("Unexpected"));
        assertTrue(involvement.none());
        List<String> lines = CrashSummary.lines("0.1.0", MODULES, involvement, mixin -> null);
        assertEquals(List.of("Version 0.1.0", "Modules: occlusionCulling on, meshWorkers off", CrashSummary.NO_CODE_LINE), lines);
    }

    @Test
    void namesTheModuleOfAMixinHandlerInTheStack() {
        CrashSummary.Involvement involvement = CrashSummary.find(List.of(
            new CrashSummary.Frame("net.minecraft.client.renderer.LevelRenderer", "handler$zza000$aervium$cullSections"),
            new CrashSummary.Frame("net.minecraft.client.renderer.LevelRenderer", "renderLevel")),
            (type, handler) -> type.endsWith("LevelRenderer") && handler.equals("aervium$cullSections")
                ? Set.of("LevelRendererOcclusionMixin") : Set.of());
        assertEquals(Set.of("LevelRendererOcclusionMixin"), involvement.mixins());
        List<String> lines = CrashSummary.lines("0.1.0", MODULES, involvement,
            mixin -> mixin.equals("LevelRendererOcclusionMixin") ? "occlusionCulling" : null);
        assertTrue(lines.contains("Aervium code in crash: LevelRendererOcclusionMixin (module occlusionCulling)"));
        assertTrue(lines.contains(CrashSummary.MODULE_MARKER + "occlusionCulling"));
        assertTrue(lines.contains("To test without it: set occlusionCulling=false in config/aervium-modules.properties and start the game again"));
    }

    @Test
    void listsOwnFramesAndAlwaysOnMixinsWithoutAModuleHint() {
        CrashSummary.Involvement involvement = CrashSummary.find(List.of(
            new CrashSummary.Frame("com.aryston.aervium.client.chunk.MeshWorkers", "executor"),
            new CrashSummary.Frame("net.minecraft.world.level.Level", "aervium$setRandValue")),
            (type, handler) -> Set.of("LevelRandomAccessor"));
        List<String> lines = CrashSummary.lines("0.1.0", MODULES, involvement, mixin -> null);
        assertTrue(lines.contains("Aervium code in crash: LevelRandomAccessor (always on)"));
        assertTrue(lines.contains("Aervium code in crash: MeshWorkers.executor"));
        assertTrue(lines.stream().noneMatch(line -> line.startsWith(CrashSummary.MODULE_MARKER)));
    }
}
