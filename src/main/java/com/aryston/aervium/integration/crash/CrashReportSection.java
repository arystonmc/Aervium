package com.aryston.aervium.integration.crash;

import com.aryston.aervium.Aervium;
import com.aryston.aervium.core.crash.CrashSummary;
import com.aryston.aervium.integration.MixinModules;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import net.neoforged.fml.ModList;

public final class CrashReportSection {
    private static final String LINE_BREAK = "\n\t\t";
    private static final String UNKNOWN_VERSION = "unknown";

    private CrashReportSection() {
    }

    public static String describe(Throwable exception) {
        CrashSummary.Involvement involvement = CrashSummary.find(frames(exception), MixinModules::mixinsDeclaring);
        List<String> lines = CrashSummary.lines(version(), MixinModules.activeModules(), involvement, MixinModules::moduleOf);
        return LINE_BREAK + String.join(LINE_BREAK, lines);
    }

    private static String version() {
        ModList mods = ModList.get();
        if (mods == null) {
            return UNKNOWN_VERSION;
        }
        return mods.getModContainerById(Aervium.MOD_ID).map(mod -> mod.getModInfo().getVersion().toString()).orElse(UNKNOWN_VERSION);
    }

    private static List<CrashSummary.Frame> frames(Throwable exception) {
        List<CrashSummary.Frame> frames = new ArrayList<>();
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Throwable current = exception; current != null && seen.add(current); current = current.getCause()) {
            for (StackTraceElement element : current.getStackTrace()) {
                frames.add(new CrashSummary.Frame(element.getClassName(), element.getMethodName()));
            }
        }
        return frames;
    }
}
