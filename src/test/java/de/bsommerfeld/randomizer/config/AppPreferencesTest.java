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

        assertTrue(preferences.getPathOverride("cs2.config.path").isEmpty());
    }

    @Test
    void roundTripsPath() throws IOException {
        Path configPath = tempDir.resolve("user_keys_default.vcfg");

        new AppPreferences(tempDir).setPathOverride("cs2.config.path", configPath);

        assertEquals(Optional.of(configPath),
                new AppPreferences(tempDir).getPathOverride("cs2.config.path"));
        assertTrue(Files.isRegularFile(tempDir.resolve("app.properties")));
    }

    @Test
    void storesKeysIndependently() throws IOException {
        AppPreferences preferences = new AppPreferences(tempDir);
        Path defaultConfig = tempDir.resolve("user_keys_default.vcfg");
        Path userConfig = tempDir.resolve("cs2_user_keys.vcfg");

        preferences.setPathOverride("cs2.config.path", defaultConfig);
        preferences.setPathOverride("cs2.userconfig.path", userConfig);

        assertEquals(Optional.of(defaultConfig), preferences.getPathOverride("cs2.config.path"));
        assertEquals(Optional.of(userConfig), preferences.getPathOverride("cs2.userconfig.path"));
    }

    @Test
    void overwritesExistingValue() throws IOException {
        AppPreferences preferences = new AppPreferences(tempDir);
        preferences.setPathOverride("cs2.config.path", tempDir.resolve("old.vcfg"));

        preferences.setPathOverride("cs2.config.path", tempDir.resolve("new.vcfg"));

        assertEquals(Optional.of(tempDir.resolve("new.vcfg")),
                preferences.getPathOverride("cs2.config.path"));
    }

    @Test
    void createsMissingBaseDirectoryOnSave() throws IOException {
        Path baseDir = tempDir.resolve("nested/randomizer-cs2");

        new AppPreferences(baseDir).setPathOverride("cs2.config.path", tempDir.resolve("config.vcfg"));

        assertTrue(Files.isRegularFile(baseDir.resolve("app.properties")));
    }
}
