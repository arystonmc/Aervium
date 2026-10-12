package com.aryston.aervium.core.crash;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Builds the Aervium part of a crash report: which modules were on and whether Aervium code ran in the crashing
 * stack, with a module to switch off when it did. The marker lines stay fixed so the crash helper on the site can
 * match them.
 */
public final class CrashSummary {
    public static final String MODULE_MARKER = "Aervium module in crash: ";
    public static final String NO_CODE_LINE = "Aervium code in crash: none";
    private static final String OWN_PACKAGE = "com.aryston.aervium.";
    private static final String HANDLER_PREFIX = "aervium$";
    private static final String SETTINGS_FILE = "config/aervium-modules.properties";
    private static final int MAX_OWN_FRAMES = 5;

    private CrashSummary() {
    }

    /** One stack frame of the crash, from the exception and every cause. */
    public record Frame(String className, String methodName) {
    }

    /**
     * Where Aervium code shows up in a stack trace.
     *
     * @param mixins       Aervium mixins whose handler ran in the stack, by simple name
     * @param ownFrames    frames in Aervium's own classes, as {@code SimpleClass.method}
     */
    public record Involvement(Set<String> mixins, Set<String> ownFrames) {
        public boolean none() {
            return mixins.isEmpty() && ownFrames.isEmpty();
        }
    }

    /**
     * @param mixinsForHandler for a frame running merged Aervium code, the Aervium mixins on that class that declare
     *                         the handler; receives the class name and the handler name starting at {@code aervium$}
     */
    public static Involvement find(List<Frame> frames, MixinLookup mixinsForHandler) {
        Set<String> mixins = new LinkedHashSet<>();
        Set<String> ownFrames = new LinkedHashSet<>();
        for (Frame frame : frames) {
            int handler = frame.methodName().indexOf(HANDLER_PREFIX);
            if (handler >= 0) {
                mixins.addAll(mixinsForHandler.find(frame.className(), frame.methodName().substring(handler)));
            } else if (frame.className().startsWith(OWN_PACKAGE) && ownFrames.size() < MAX_OWN_FRAMES) {
                String simpleName = frame.className().substring(frame.className().lastIndexOf('.') + 1);
                ownFrames.add(simpleName + "." + frame.methodName());
            }
        }
        return new Involvement(mixins, ownFrames);
    }

    /**
     * @param modules       every module key with whether it is on, in display order
     * @param moduleOfMixin module key of a mixin, or null when the mixin is always on
     */
    public static List<String> lines(String version, Map<String, Boolean> modules, Involvement involvement,
                                     Function<String, String> moduleOfMixin) {
        List<String> lines = new ArrayList<>();
        lines.add("Version " + version);
        List<String> states = new ArrayList<>();
        modules.forEach((key, on) -> states.add(key + (on ? " on" : " off")));
        lines.add("Modules: " + String.join(", ", states));
        if (involvement.none()) {
            lines.add(NO_CODE_LINE);
            return lines;
        }
        Set<String> crashModules = new LinkedHashSet<>();
        for (String mixin : involvement.mixins()) {
            String module = moduleOfMixin.apply(mixin);
            lines.add("Aervium code in crash: " + mixin + (module == null ? " (always on)" : " (module " + module + ")"));
            if (module != null) {
                crashModules.add(module);
            }
        }
        for (String frame : involvement.ownFrames()) {
            lines.add("Aervium code in crash: " + frame);
        }
        for (String module : crashModules) {
            lines.add(MODULE_MARKER + module);
        }
        if (!crashModules.isEmpty()) {
            lines.add("To test without it: set " + String.join("=false, ", crashModules) + "=false in " + SETTINGS_FILE
                + " and start the game again");
        }
        return lines;
    }

    @FunctionalInterface
    public interface MixinLookup {
        Set<String> find(String className, String handlerName);
    }
}
