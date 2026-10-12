package com.aryston.aervium.client;

import com.aryston.aervium.Aervium;
import com.aryston.aervium.client.chunk.LeafCulling;
import com.aryston.aervium.config.AerviumConfig;
import com.aryston.aervium.core.hardware.GpuKind;
import com.aryston.aervium.core.hardware.HardwareClassifier;
import com.aryston.aervium.core.hardware.HardwareProfile;
import com.aryston.aervium.core.hardware.HardwareTier;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.device.DeviceInfo;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.loading.FMLPaths;

final class HardwareSetup {
    private static final String RECORD_FILE = "aervium-hardware.properties";
    private static final String TIER_KEY = "tier=";

    private HardwareSetup() {
    }

    static void onGameLoaded(Minecraft minecraft, boolean benchmarkRun) {
        HardwareProfile profile = detect();
        HardwareTier tier = HardwareClassifier.classify(profile);
        Path record = FMLPaths.CONFIGDIR.get().resolve(RECORD_FILE);
        if (Files.exists(record)) {
            Aervium.LOGGER.info("Aervium hardware class {} ({}); first launch settings already applied", tier.key(), profile.describe());
            return;
        }
        if (benchmarkRun) {
            Aervium.LOGGER.info("Aervium hardware class {} ({}); benchmark run, settings left as they are", tier.key(), profile.describe());
            return;
        }
        List<String> changes = apply(minecraft.options, tier);
        write(record, tier, profile, changes);
        Aervium.LOGGER.info("Aervium hardware class {} ({}), first launch changes: {}", tier.key(), profile.describe(),
            changes.isEmpty() ? "none" : String.join(", ", changes));
        if (!changes.isEmpty()) {
            SystemToast.add(minecraft.gui.toastManager(), SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
                Component.translatable("aervium.hardware.title"),
                Component.translatable("aervium.hardware." + tier.key()));
        }
    }

    private static HardwareProfile detect() {
        DeviceInfo device = RenderSystem.getDevice().getDeviceInfo();
        long physical = ManagementFactory.getOperatingSystemMXBean() instanceof com.sun.management.OperatingSystemMXBean system
            ? system.getTotalMemorySize()
            : HardwareProfile.UNKNOWN_BYTES;
        return new HardwareProfile(
            Runtime.getRuntime().availableProcessors(),
            Runtime.getRuntime().maxMemory(),
            physical,
            device.name(),
            kind(device),
            VulkanMemoryProbe.deviceLocalBytes()
        );
    }

    private static GpuKind kind(DeviceInfo device) {
        return switch (device.type()) {
            case DISCRETE -> GpuKind.DISCRETE;
            case INTEGRATED -> GpuKind.INTEGRATED;
            case CPU -> GpuKind.SOFTWARE;
            case VIRTUAL, OTHER -> GpuKind.UNKNOWN;
        };
    }

    private static List<String> apply(Options options, HardwareTier tier) {
        List<String> changes = new ArrayList<>();
        int render = options.renderDistance().get();
        if (tier.renderDistance(render) < render) {
            options.renderDistance().set(tier.renderDistance(render));
            changes.add("render distance " + render + " -> " + options.renderDistance().get());
        }
        int simulation = options.simulationDistance().get();
        if (tier.simulationDistance(simulation) < simulation) {
            options.simulationDistance().set(tier.simulationDistance(simulation));
            changes.add("simulation distance " + simulation + " -> " + options.simulationDistance().get());
        }
        if (!changes.isEmpty()) {
            options.save();
        }
        if (tier.leafCulling() && !AerviumConfig.LEAF_CULLING.getAsBoolean()) {
            AerviumConfig.LEAF_CULLING.set(true);
            AerviumConfig.LEAF_CULLING.save();
            LeafCulling.setEnabled(true);
            changes.add("leaf culling on");
        }
        return changes;
    }

    private static void write(Path record, HardwareTier tier, HardwareProfile profile, List<String> changes) {
        List<String> lines = List.of(
            "# Written by Aervium on its first launch. Delete this file to detect the hardware again on the next launch.",
            "# " + profile.describe(),
            "# Changes: " + (changes.isEmpty() ? "none" : String.join(", ", changes)),
            TIER_KEY + tier.key()
        );
        try {
            Files.write(record, lines);
        } catch (IOException exception) {
            Aervium.LOGGER.warn("Aervium could not write {}", record, exception);
        }
    }
}
