package com.aryston.aervium.core.cache;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.SequencedSet;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ShaderIncludes {
    private static final Pattern INCLUDE = Pattern.compile("^\\s*#\\s*(?:include|moj_import)\\s*[<\"]([^>\"]+)[>\"]", Pattern.MULTILINE);

    private ShaderIncludes() {
    }

    public static SequencedSet<String> direct(String source) {
        SequencedSet<String> includes = new LinkedHashSet<>();
        Matcher matcher = INCLUDE.matcher(source);
        while (matcher.find()) {
            includes.add(matcher.group(1));
        }
        return includes;
    }

    public static SequencedSet<String> transitive(String source, Function<String, Optional<String>> includeSource) {
        SequencedSet<String> visited = new LinkedHashSet<>();
        Deque<String> pending = new ArrayDeque<>(direct(source));
        while (!pending.isEmpty()) {
            String include = pending.removeFirst();
            if (visited.add(include)) {
                includeSource.apply(include).ifPresent(content -> pending.addAll(direct(content)));
            }
        }
        return visited;
    }
}
