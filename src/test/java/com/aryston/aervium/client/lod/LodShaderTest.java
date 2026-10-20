package com.aryston.aervium.client.lod;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.shaderc.Shaderc;

class LodShaderTest {
    private static final Pattern INCLUDE = Pattern.compile("(?m)^#include\\s+<([^>]+)>\\s*$");
    private static final String ROOT = "assets/aervium/shaders/lod/";

    @Test
    void forwardAndGeometryShadersCompileToSpirvWithTheActualIncludes() throws IOException {
        compile("distant_terrain.vsh", Shaderc.shaderc_vertex_shader);
        compile("distant_terrain.fsh", Shaderc.shaderc_fragment_shader);
        compile("distant_terrain_geometry.fsh", Shaderc.shaderc_fragment_shader);
    }

    private static void compile(String file, int kind) throws IOException {
        String source = expand(ROOT + file, new HashSet<>()).replace("#version 330", "#version 450");
        long compiler = Shaderc.shaderc_compiler_initialize();
        long options = Shaderc.shaderc_compile_options_initialize();
        try {
            Shaderc.shaderc_compile_options_set_auto_bind_uniforms(options, true);
            Shaderc.shaderc_compile_options_set_auto_map_locations(options, true);
            long result = Shaderc.shaderc_compile_into_spv(compiler, source, kind, file, "main", options);
            try {
                assertEquals(Shaderc.shaderc_compilation_status_success, Shaderc.shaderc_result_get_compilation_status(result),
                    Shaderc.shaderc_result_get_error_message(result));
            } finally {
                Shaderc.shaderc_result_release(result);
            }
        } finally {
            Shaderc.shaderc_compile_options_release(options);
            Shaderc.shaderc_compiler_release(compiler);
        }
    }

    private static String expand(String path, Set<String> included) throws IOException {
        if (!included.add(path)) {
            return "";
        }
        String source;
        try (var input = LodShaderTest.class.getClassLoader().getResourceAsStream(path)) {
            assertNotNull(input, path);
            source = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        var matcher = INCLUDE.matcher(source);
        StringBuilder expanded = new StringBuilder();
        while (matcher.find()) {
            String[] id = matcher.group(1).split(":", 2);
            String resource = "assets/" + id[0] + "/shaders/include/" + id[1];
            matcher.appendReplacement(expanded, java.util.regex.Matcher.quoteReplacement(expand(resource, included)));
        }
        matcher.appendTail(expanded);
        return expanded.toString();
    }
}
