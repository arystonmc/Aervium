package com.aryston.aervium.integration;

import com.aryston.aervium.Aervium;
import com.aryston.aervium.core.module.ModuleSettings;
import com.aryston.aervium.core.module.OptimizationModule;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public final class MixinModules implements IMixinConfigPlugin {
    private static final String SETTINGS_FILE = "aervium-modules.properties";
    private static final char PACKAGE_SEPARATOR = '.';
    private static final Map<String, OptimizationModule> MIXIN_MODULES = Map.of(
        "GlslCompilerMixin", OptimizationModule.SHADER_CACHE,
        "VulkanRenderPipelineMixin", OptimizationModule.PIPELINE_CACHE,
        "RenderSystemMixin", OptimizationModule.PIPELINE_WARMUP,
        "PalettedContainerMixin", OptimizationModule.COMPACT_CHUNK_LOCKS,
        "CompiledSectionMeshMixin", OptimizationModule.SECTION_SLICE_CACHE,
        "UberGpuBufferMixin", OptimizationModule.SECTION_SLICE_CACHE,
        "LevelRendererSectionSliceMixin", OptimizationModule.SECTION_SLICE_CACHE,
        "DataLayerStorageMapMixin", OptimizationModule.LIGHT_SNAPSHOTS,
        "BlockDataLayerStorageMapMixin", OptimizationModule.LIGHT_SNAPSHOTS,
        "SkyDataLayerStorageMapMixin", OptimizationModule.LIGHT_SNAPSHOTS
    );

    private ModuleSettings settings;
    private boolean rendererReplaced;

    @Override
    public void onLoad(String mixinPackage) {
        settings = readSettings(FMLPaths.CONFIGDIR.get().resolve(SETTINGS_FILE));
        rendererReplaced = CompatibilityGuard.REPLACED_RENDERERS.stream().anyMatch(id -> FMLLoader.getCurrent().getLoadingModList().getModFileById(id) != null);
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        OptimizationModule module = MIXIN_MODULES.get(mixinClassName.substring(mixinClassName.lastIndexOf(PACKAGE_SEPARATOR) + 1));
        if (module == null) {
            return true;
        }
        return settings.isEnabled(module) && !(module.renderer() && rendererReplaced);
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    private static ModuleSettings readSettings(Path file) {
        try {
            ModuleSettings settings = ModuleSettings.parse(Files.exists(file) ? Files.readAllLines(file, StandardCharsets.UTF_8) : List.of());
            Files.write(file, settings.lines(), StandardCharsets.UTF_8);
            return settings;
        } catch (IOException failure) {
            Aervium.LOGGER.warn("Aervium could not read {}; every module stays on", file, failure);
            return ModuleSettings.parse(List.of());
        }
    }
}
