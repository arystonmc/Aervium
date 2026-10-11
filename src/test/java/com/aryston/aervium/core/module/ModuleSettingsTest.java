package com.aryston.aervium.core.module;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class ModuleSettingsTest {
    @Test
    void everyModuleIsOnByDefault() {
        ModuleSettings settings = ModuleSettings.parse(List.of());
        Arrays.stream(OptimizationModule.values()).forEach(module -> assertTrue(settings.isEnabled(module)));
    }

    @Test
    void readsSwitchedOffModulesAndIgnoresUnknownLines() {
        ModuleSettings settings = ModuleSettings.parse(List.of("shaderCache = false", "unknown=false", "garbage", "pipelineCache=TRUE"));
        assertFalse(settings.isEnabled(OptimizationModule.SHADER_CACHE));
        assertTrue(settings.isEnabled(OptimizationModule.PIPELINE_CACHE));
        assertTrue(settings.isEnabled(OptimizationModule.COMPACT_CHUNK_LOCKS));
    }

    @Test
    void writesEveryModuleSoPlayersSeeTheSwitches() {
        List<String> lines = ModuleSettings.parse(List.of("pipelineWarmup=false")).lines();
        assertEquals(OptimizationModule.values().length, lines.size());
        assertTrue(lines.contains("pipelineWarmup=false"));
        assertEquals(lines, ModuleSettings.parse(lines).lines());
    }
}
