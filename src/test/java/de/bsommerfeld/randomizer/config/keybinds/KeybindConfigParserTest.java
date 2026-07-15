package de.bsommerfeld.randomizer.config.keybinds;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class KeybindConfigParserTest {

    @TempDir
    Path tempDir;

    private final KeybindConfigParser parser = new KeybindConfigParser();

    private Path fixtureConfig() throws Exception {
        return Path.of(getClass().getResource("/fixtures/user_keys_default.vcfg").toURI());
    }

    @Test
    void convertsVcfgToPrettyJson() throws Exception {
        KeybindConfig loaded = parser.parse(fixtureConfig());

        assertTrue(loaded.prettyJson().contains("\"config\""));
        assertTrue(loaded.prettyJson().contains("\"bindings\""));
        assertTrue(loaded.prettyJson().contains("\"w\": \"+forward\""));
    }

    @Test
    void displaysBackslashKeyUnescaped() throws Exception {
        Path config = tempDir.resolve("cs2_user_keys.vcfg");
        Files.writeString(config, "\"bindings\"\n{\n\t\"\\\"\t\t\"toggleconsole\"\n}");

        KeybindConfig loaded = parser.parse(config);

        // The backslash key is displayed unescaped: "\" instead of "\\"
        assertTrue(loaded.prettyJson().contains("\"\\\": \"toggleconsole\""));
    }
}
