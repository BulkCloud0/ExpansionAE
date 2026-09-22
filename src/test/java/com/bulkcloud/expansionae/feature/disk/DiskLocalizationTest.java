package com.bulkcloud.expansionae.feature.disk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

final class DiskLocalizationTest {
    private static final Gson GSON = new Gson();
    private static final Pattern PLACEHOLDER = Pattern.compile("%(?:\\d+\\$)?s");

    private static final Set<String> REQUIRED_KEYS = new HashSet<>(Arrays.asList(
            "itemGroup.expansionae",
            "item.expansionae.1k_disk",
            "item.expansionae.4k_disk",
            "item.expansionae.16k_disk",
            "item.expansionae.64k_disk",
            "tooltip.expansionae.disk.items",
            "tooltip.expansionae.disk.types",
            "tooltip.expansionae.disk.no_type_limit"));

    @Test
    void englishAndBrazilianPortugueseHaveMatchingDiskKeysAndFormats() throws IOException {
        Map<String, String> english = load("en_us");
        Map<String, String> portuguese = load("pt_br");

        assertEquals(english.keySet(), portuguese.keySet(),
                "en_us and pt_br must expose the same localization keys");
        assertEquals(REQUIRED_KEYS, english.keySet(),
                "DISK localization resources should contain exactly the expected keys");

        for (String key : REQUIRED_KEYS) {
            String en = english.get(key);
            String pt = portuguese.get(key);

            assertNotNull(en, "Missing en_us value for " + key);
            assertNotNull(pt, "Missing pt_br value for " + key);
            assertFalse(en.trim().isEmpty(), "Empty en_us value for " + key);
            assertFalse(pt.trim().isEmpty(), "Empty pt_br value for " + key);
            assertEquals(countPlaceholders(en), countPlaceholders(pt),
                    "Placeholder count mismatch for " + key);
        }

        assertEquals(2, countPlaceholders(english.get("tooltip.expansionae.disk.items")));
        assertEquals(1, countPlaceholders(english.get("tooltip.expansionae.disk.types")));
        assertEquals(0, countPlaceholders(english.get("tooltip.expansionae.disk.no_type_limit")));

        for (String tier : Arrays.asList("1k", "4k", "16k", "64k")) {
            String key = "item.expansionae." + tier + "_disk";
            assertTrue(english.get(key).contains(tier), "en_us item name lost tier " + tier);
            assertTrue(portuguese.get(key).contains(tier), "pt_br item name lost tier " + tier);
        }
    }

    private static Map<String, String> load(String locale) throws IOException {
        String resource = "/assets/expansionae/lang/" + locale + ".json";
        try (InputStream stream = DiskLocalizationTest.class.getResourceAsStream(resource)) {
            assertNotNull(stream, "Missing localization resource " + resource);
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return GSON.fromJson(
                        reader,
                        new TypeToken<Map<String, String>>() { }.getType());
            }
        }
    }

    private static int countPlaceholders(String value) {
        int count = 0;
        Matcher matcher = PLACEHOLDER.matcher(value);
        while (matcher.find()) {
            count++;
        }
        return count;
    }
}
