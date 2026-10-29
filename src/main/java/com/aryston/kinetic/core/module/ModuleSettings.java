package com.aryston.kinetic.core.module;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class ModuleSettings {
    private static final String ASSIGNMENT = "=";
    private static final String ENABLED = "true";
    private static final String DISABLED = "false";

    private final Map<OptimizationModule, Boolean> enabled;

    private ModuleSettings(Map<OptimizationModule, Boolean> enabled) {
        this.enabled = enabled;
    }

    public static ModuleSettings parse(List<String> lines) {
        Map<OptimizationModule, Boolean> enabled = new EnumMap<>(OptimizationModule.class);
        Arrays.stream(OptimizationModule.values()).forEach(module -> enabled.put(module, true));
        for (String line : lines) {
            String[] parts = line.split(ASSIGNMENT, 2);
            if (parts.length == 2) {
                Arrays.stream(OptimizationModule.values())
                    .filter(module -> module.key().equals(parts[0].trim()))
                    .findFirst()
                    .ifPresent(module -> enabled.put(module, !DISABLED.equalsIgnoreCase(parts[1].trim())));
            }
        }
        return new ModuleSettings(enabled);
    }

    public boolean isEnabled(OptimizationModule module) {
        return enabled.get(module);
    }

    public ModuleSettings with(OptimizationModule module, boolean value) {
        Map<OptimizationModule, Boolean> changed = new EnumMap<>(enabled);
        changed.put(module, value);
        return new ModuleSettings(changed);
    }

    public List<String> lines() {
        return Arrays.stream(OptimizationModule.values())
            .map(module -> module.key() + ASSIGNMENT + (isEnabled(module) ? ENABLED : DISABLED))
            .collect(Collectors.toList());
    }
}
