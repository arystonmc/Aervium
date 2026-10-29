package com.aryston.kinetic.core.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class ShaderCacheKeyTest {
    private static final String SOURCE = "#version 330\n#include <minecraft:fog.glsl>\n  #include \"minecraft:globals.glsl\"\nvoid main() {}\n";

    @Test
    void findsDirectAndTransitiveIncludes() {
        Map<String, String> includes = Map.of(
            "minecraft:fog.glsl", "#include <minecraft:projection.glsl>\n",
            "minecraft:globals.glsl", "",
            "minecraft:projection.glsl", "#include <minecraft:fog.glsl>\n"
        );
        assertEquals(List.of("minecraft:fog.glsl", "minecraft:globals.glsl"), List.copyOf(ShaderIncludes.direct(SOURCE)));
        assertEquals(
            List.of("minecraft:fog.glsl", "minecraft:globals.glsl", "minecraft:projection.glsl"),
            List.copyOf(ShaderIncludes.transitive(SOURCE, lookup(includes)))
        );
    }

    @Test
    void theSameInputGivesTheSameKey() {
        Map<String, String> includes = Map.of("minecraft:fog.glsl", "fog", "minecraft:globals.glsl", "globals");
        assertEquals(key(input(Map.of("A", "1", "B", "2")), includes), key(input(Map.of("B", "2", "A", "1")), includes));
    }

    @Test
    void anyChangeThatAffectsTheBinaryChangesTheKey() {
        Map<String, String> includes = new HashMap<>(Map.of("minecraft:fog.glsl", "fog", "minecraft:globals.glsl", "globals"));
        String original = key(input(Map.of()), includes);
        assertNotEquals(original, key(input(Map.of("A", "1")), includes));
        assertNotEquals(original, key(withFlags(Set.of("ALPHA_CUTOUT")), includes));
        assertNotEquals(original, key(withDepth(false), includes));
        includes.put("minecraft:globals.glsl", "changed globals");
        assertNotEquals(original, key(input(Map.of()), includes));
    }

    @Test
    void aMissingIncludeStillGivesAKey() {
        assertEquals(64, key(input(Map.of()), Map.of()).length());
    }

    private static String key(ShaderCacheKey.Input input, Map<String, String> includes) {
        return ShaderCacheKey.of(input, lookup(includes));
    }

    private static Function<String, Optional<String>> lookup(Map<String, String> includes) {
        return include -> Optional.ofNullable(includes.get(include));
    }

    private static ShaderCacheKey.Input input(Map<String, String> defines) {
        return new ShaderCacheKey.Input("26.3", "minecraft:core/terrain", "VERTEX", SOURCE, defines, Set.of(), true, false, true);
    }

    private static ShaderCacheKey.Input withFlags(Set<String> flags) {
        return new ShaderCacheKey.Input("26.3", "minecraft:core/terrain", "VERTEX", SOURCE, Map.of(), flags, true, false, true);
    }

    private static ShaderCacheKey.Input withDepth(boolean zeroToOne) {
        return new ShaderCacheKey.Input("26.3", "minecraft:core/terrain", "VERTEX", SOURCE, Map.of(), Set.of(), zeroToOne, false, true);
    }
}
