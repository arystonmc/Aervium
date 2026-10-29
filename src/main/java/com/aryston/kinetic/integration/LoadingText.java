package com.aryston.kinetic.integration;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.neoforged.fml.loading.FMLPaths;

final class LoadingText {
    private static final String OPTIONS_FILE = "options.txt";
    private static final String LANGUAGE_PREFIX = "lang:";
    private static final String DEFAULT_LANGUAGE = "en_us";
    private static final String LANGUAGE_FILE = "/assets/kinetic/lang/%s.json";
    private static final String QUOTE = "'";
    private static final String ESCAPED_QUOTE = "''";

    private LoadingText() {
    }

    static String of(String key) {
        String text = translation(language(), key).or(() -> translation(DEFAULT_LANGUAGE, key)).orElse(key);
        return text.replace(QUOTE, ESCAPED_QUOTE);
    }

    private static String language() {
        Path options = FMLPaths.GAMEDIR.get().resolve(OPTIONS_FILE);
        try {
            List<String> lines = Files.exists(options) ? Files.readAllLines(options, StandardCharsets.UTF_8) : List.of();
            return lines.stream()
                .filter(line -> line.startsWith(LANGUAGE_PREFIX))
                .map(line -> line.substring(LANGUAGE_PREFIX.length()).trim().toLowerCase(Locale.ROOT))
                .findFirst()
                .orElse(DEFAULT_LANGUAGE);
        } catch (IOException unreadable) {
            return DEFAULT_LANGUAGE;
        }
    }

    private static Optional<String> translation(String language, String key) {
        try (InputStream stream = LoadingText.class.getResourceAsStream(String.format(Locale.ROOT, LANGUAGE_FILE, language))) {
            if (stream == null) {
                return Optional.empty();
            }
            JsonObject entries = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            return entries.has(key) ? Optional.of(entries.get(key).getAsString()) : Optional.empty();
        } catch (IOException | RuntimeException unreadable) {
            return Optional.empty();
        }
    }
}
