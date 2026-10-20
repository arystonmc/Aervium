package com.aryston.aervium.client;

import com.aryston.aervium.Aervium;
import com.aryston.aervium.config.AerviumConfig;
import com.aryston.aervium.core.telemetry.TelemetrySnapshot;
import java.util.List;
import net.minecraft.client.gui.components.debug.DebugScreenDisplayer;
import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.client.gui.components.debug.DebugScreenEntryStatus;
import net.minecraft.client.gui.components.debug.DebugScreenProfile;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.client.event.RegisterDebugEntriesEvent;
import org.jspecify.annotations.Nullable;

final class AerviumDebugEntry implements DebugScreenEntry {
    private static final Identifier ID = Identifier.fromNamespaceAndPath(Aervium.MOD_ID, "performance");

    private final List<String> passiveRendererMods;

    private AerviumDebugEntry(List<String> passiveRendererMods) {
        this.passiveRendererMods = passiveRendererMods;
    }

    static void register(RegisterDebugEntriesEvent event, List<String> passiveRendererMods) {
        event.register(ID, new AerviumDebugEntry(passiveRendererMods));
        event.includeInProfile(ID, DebugScreenProfile.DEFAULT, DebugScreenEntryStatus.IN_OVERLAY);
    }

    @Override
    public void display(DebugScreenDisplayer displayer, @Nullable Level level, @Nullable LevelChunk clientChunk, @Nullable LevelChunk serverChunk) {
        TelemetrySnapshot snapshot = ClientEvents.latestSnapshot();
        if (snapshot == null || !AerviumConfig.OVERLAY_VISIBLE.getAsBoolean()) {
            return;
        }
        displayer.addToGroup(ID, OverlayLines.of(snapshot, passiveRendererMods).stream().map(OverlayLines.Line::text).toList());
    }
}
