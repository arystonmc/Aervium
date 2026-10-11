package com.aryston.aervium.client.benchmark;

import com.aryston.aervium.Aervium;
import com.aryston.aervium.core.telemetry.ByteSizes;
import com.aryston.aervium.debug.benchmark.BenchmarkEnvironment;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.device.DeviceInfo;
import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;

final class BenchmarkEnvironmentProbe {
    private static final String UNKNOWN = "unknown";

    private BenchmarkEnvironmentProbe() {
    }

    static BenchmarkEnvironment capture(Minecraft minecraft) {
        Options options = minecraft.options;
        Window window = minecraft.getWindow();
        DeviceInfo device = RenderSystem.getDevice().getDeviceInfo();
        return new BenchmarkEnvironment(
            modVersion(Aervium.MOD_ID),
            SharedConstants.getCurrentVersion().name(),
            Runtime.version().toString(),
            System.getProperty("os.name") + " " + System.getProperty("os.version"),
            Runtime.getRuntime().availableProcessors(),
            ByteSizes.megabytes(Runtime.getRuntime().maxMemory()),
            device.name(),
            device.backendName(),
            window.getWidth(),
            window.getHeight(),
            options.renderDistance().get(),
            options.simulationDistance().get(),
            options.framerateLimit().get(),
            options.enableVsync().get(),
            BenchmarkWorld.SEED,
            installedMods()
        );
    }

    private static String modVersion(String modId) {
        return ModList.get().getModContainerById(modId)
            .map(container -> container.getModInfo().getVersion().toString())
            .orElse(UNKNOWN);
    }

    private static List<String> installedMods() {
        return ModList.get().getSortedMods().stream()
            .map(BenchmarkEnvironmentProbe::describe)
            .toList();
    }

    private static String describe(ModContainer container) {
        return container.getModId() + " " + container.getModInfo().getVersion();
    }
}
