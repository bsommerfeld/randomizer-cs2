package de.bsommerfeld.randomizer.ui.config;

import de.bsommerfeld.randomizer.config.keybinds.KeybindConfig;
import de.bsommerfeld.randomizer.vdf.VdfParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The decisions behind the config tabs, checked without starting JavaFX. */
class ConfigTabsTest {

    private static final String FILE_NAME = "cs2_user_keys.vcfg";

    @TempDir
    Path tempDir;

    @Test
    void acceptsAnExistingFileOfTheExpectedName() throws IOException {
        Path file = Files.writeString(tempDir.resolve(FILE_NAME), "\"config\"\n{\n}\n");

        assertEquals(Optional.empty(), ConfigTabController.problemWith(file.toString(), FILE_NAME));
        assertEquals(Optional.empty(),
                ConfigTabController.problemWith(file.toString(), FILE_NAME.toUpperCase()), "name check ignores case");
    }

    @Test
    void namesTheProblemWithAnUnusablePath() throws IOException {
        Path otherName = Files.writeString(tempDir.resolve("other.vcfg"), "");

        assertEquals(Optional.of("Please enter a path."), ConfigTabController.problemWith("", FILE_NAME));
        assertEquals(Optional.of("File not found: " + tempDir.resolve("missing.vcfg")),
                ConfigTabController.problemWith(tempDir.resolve("missing.vcfg").toString(), FILE_NAME));
        assertEquals(Optional.of("This tab only takes " + FILE_NAME + "."),
                ConfigTabController.problemWith(otherName.toString(), FILE_NAME));
    }

    @Test
    void mergedViewLaysCustomOverDefaults() {
        MergedConfigTabController.View view = MergedConfigTabController.merge(
                Optional.of(config("\"config\" { \"bindings\" { \"c\" \"+radialradio\" \"w\" \"+forward\" } }")),
                Optional.of(config("\"config\" { \"bindings\" { \"c\" \"player_ping\" } }")));

        assertTrue(view.json().contains("\"c\": \"player_ping\""), view.json());
        assertTrue(view.json().contains("\"w\": \"+forward\""), view.json());
    }

    @Test
    void mergedViewFallsBackToWhicheverConfigIsThere() {
        KeybindConfig defaults = config("\"config\" { \"bindings\" { \"w\" \"+forward\" } }");

        MergedConfigTabController.View onlyDefaults = MergedConfigTabController.merge(Optional.of(defaults), Optional.empty());
        MergedConfigTabController.View nothing = MergedConfigTabController.merge(Optional.empty(), Optional.empty());

        assertEquals(defaults.prettyJson(), onlyDefaults.json());
        assertTrue(onlyDefaults.status().startsWith("Custom keybinds not loaded"), onlyDefaults.status());
        assertEquals("", nothing.json());
        assertTrue(nothing.status().startsWith("No config loaded"), nothing.status());
    }

    private static KeybindConfig config(String vdf) {
        return new KeybindConfig(Path.of("test.vcfg"), VdfParser.parse(vdf));
    }
}
