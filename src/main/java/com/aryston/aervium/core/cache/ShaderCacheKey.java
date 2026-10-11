package com.aryston.aervium.core.cache;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;

public final class ShaderCacheKey {
    private static final String FORMAT = "aervium-spirv-1";
    private static final String MISSING_INCLUDE = "missing";

    private ShaderCacheKey() {
    }

    public static String of(Input input, Function<String, Optional<String>> includeSource) {
        ContentKey key = ContentKey.create()
            .add(FORMAT)
            .add(input.gameVersion())
            .add(input.name())
            .add(input.stage())
            .add(input.source())
            .add(input.depthZeroToOne())
            .add(input.explicitDepthInvariance())
            .add(input.drawParameters());
        for (String include : ShaderIncludes.transitive(input.source(), includeSource)) {
            key.add(include).add(includeSource.apply(include).orElse(MISSING_INCLUDE));
        }
        new TreeMap<>(input.defines()).forEach((name, value) -> key.add(name).add(value));
        new TreeSet<>(input.flags()).forEach(key::add);
        return key.hex();
    }

    public record Input(
        String gameVersion,
        String name,
        String stage,
        String source,
        Map<String, String> defines,
        Set<String> flags,
        boolean depthZeroToOne,
        boolean explicitDepthInvariance,
        boolean drawParameters
    ) {
    }
}
