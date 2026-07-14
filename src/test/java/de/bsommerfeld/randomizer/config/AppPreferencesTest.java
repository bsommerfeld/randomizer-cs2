package de.bsommerfeld.randomizer.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppPreferencesTest {

    @TempDir
    Path tempDir;

    @Test
    void returnsEmptyWithoutStoredValue() {
        AppPreferences preferences = new AppPreferences(tempDir);

        assertTrue(preferences.getConfigPathOverride(ConfigKind.DEFAULT).isEmpty());
        assertTrue(preferences.getConfigPathOverride(ConfigKind.USER).isEmpty());
    }

    @Test
    void roundTripsConfigPath() throws IOException {
        Path configPath = tempDir.resolve("user_keys_default.vcfg");

        new AppPreferences(tempDir).setConfigPathOverride(ConfigKind.DEFAULT, configPath);

        assertEquals(Optional.of(configPath),
                new AppPreferences(tempDir).getConfigPathOverride(ConfigKind.DEFAULT));
        assertTrue(Files.isRegularFile(tempDir.resolve("app.properties")));
    }

    @Test
    void storesBothKindsIndependently() throws IOException {
        AppPreferences preferences = new AppPreferences(tempDir);
        Path defaultConfig = tempDir.resolve("user_keys_default.vcfg");
        Path userConfig = tempDir.resolve("cs2_user_keys.vcfg");

        preferences.setConfigPathOverride(ConfigKind.DEFAULT, defaultConfig);
        preferences.setConfigPathOverride(ConfigKind.USER, userConfig);

        assertEquals(Optional.of(defaultConfig), preferences.getConfigPathOverride(ConfigKind.DEFAULT));
        assertEquals(Optional.of(userConfig), preferences.getConfigPathOverride(ConfigKind.USER));
    }

    @Test
    void overwritesExistingValue() throws IOException {
        AppPreferences preferences = new AppPreferences(tempDir);
        preferences.setConfigPathOverride(ConfigKind.DEFAULT, tempDir.resolve("old.vcfg"));

        preferences.setConfigPathOverride(ConfigKind.DEFAULT, tempDir.resolve("new.vcfg"));

        assertEquals(Optional.of(tempDir.resolve("new.vcfg")),
                preferences.getConfigPathOverride(ConfigKind.DEFAULT));
    }

    @Test
    void createsMissingBaseDirectoryOnSave() throws IOException {
        Path baseDir = tempDir.resolve("nested/randomizer-cs2");

        new AppPreferences(baseDir).setConfigPathOverride(ConfigKind.DEFAULT, tempDir.resolve("config.vcfg"));

        assertTrue(Files.isRegularFile(baseDir.resolve("app.properties")));
    }
}
