package com.aryston.aervium;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.junit.jupiter.api.Test;

class LanguageFilesTest {
    private static final String LANGUAGE_FOLDER = "/assets/aervium/lang/";
    private static final String ENGLISH = "en_us.json";
    private static final String TURKISH = "tr_tr.json";
    private static final String CONFIG_PREFIX = "aervium.configuration.";
    private static final String SCREEN_PREFIX = "aervium.configuration.section.";
    private static final String SCREEN_TITLE = "aervium.configuration.title";
    private static final String TOOLTIP = ".tooltip";

    @Test
    void everyLanguageHasTheSameKeys() throws IOException {
        assertEquals(keys(ENGLISH), keys(TURKISH));
    }

    @Test
    void everyConfigEntryHasATooltip() throws IOException {
        Set<String> keys = keys(ENGLISH);
        keys.stream()
            .filter(key -> key.startsWith(CONFIG_PREFIX))
            .filter(key -> !key.startsWith(SCREEN_PREFIX) && !key.equals(SCREEN_TITLE) && !key.endsWith(TOOLTIP))
            .forEach(entry -> assertTrue(keys.contains(entry + TOOLTIP), entry + TOOLTIP));
    }

    private static Set<String> keys(String file) throws IOException {
        try (InputStream stream = LanguageFilesTest.class.getResourceAsStream(LANGUAGE_FOLDER + file)) {
            assertNotNull(stream, file);
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                JsonObject language = JsonParser.parseReader(reader).getAsJsonObject();
                return Set.copyOf(language.keySet());
            }
        }
    }
}
