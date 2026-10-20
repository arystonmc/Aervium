package com.aryston.aervium.client.config;

import com.aryston.aervium.Aervium;
import com.aryston.aervium.core.module.ModuleSettings;
import com.aryston.aervium.core.module.OptimizationModule;
import com.aryston.aervium.integration.MixinModules;
import com.aryston.arkea.api.config.Binding;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.neoforged.fml.loading.FMLPaths;

final class ModuleFile {
    private final Path file = FMLPaths.CONFIGDIR.get().resolve(MixinModules.SETTINGS_FILE);
    private ModuleSettings settings = read();

    Binding<Boolean> binding(OptimizationModule module) {
        return new Binding<>() {
            @Override
            public Boolean get() {
                return settings.isEnabled(module);
            }

            @Override
            public void set(Boolean value) {
                settings = settings.with(module, value);
            }

            @Override
            public Boolean defaultValue() {
                return true;
            }

            @Override
            public void save() {
                write();
            }

            @Override
            public boolean requiresRestart() {
                return true;
            }
        };
    }

    private ModuleSettings read() {
        try {
            return ModuleSettings.parse(Files.exists(file) ? Files.readAllLines(file, StandardCharsets.UTF_8) : List.of());
        } catch (IOException failure) {
            Aervium.LOGGER.warn("Aervium could not read {}", file, failure);
            return ModuleSettings.parse(List.of());
        }
    }

    private void write() {
        try {
            Files.write(file, settings.lines(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            Aervium.LOGGER.warn("Aervium could not write {}", file, failure);
        }
    }
}
