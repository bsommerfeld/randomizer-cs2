package de.bsommerfeld.randomizer.config;

import de.bsommerfeld.randomizer.config.keybinds.KeybindConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigRepositoryTest {

    private static final String PREFERENCE_KEY = "test.path";

    @TempDir
    Path tempDir;

    private AppPreferences preferences;

    /** A repository whose auto-detection yields exactly {@code detected}, or nothing for null. */
    private ConfigRepository repository(Path detected) {
        preferences = new AppPreferences(tempDir.resolve("prefs"));
        return new ConfigRepository(
                new ConfigSource(PREFERENCE_KEY, "test.vcfg", () -> Optional.ofNullable(detected)), preferences);
    }

    /** A config file whose only entry names it, so a test can tell which file was loaded. */
    private Path fileWith(String name, String id) throws IOException {
        Path file = tempDir.resolve(name);
        Files.writeString(file, "\"id\" \"" + id + "\"");
        return file;
    }

    private static Optional<String> idOf(Optional<KeybindConfig> config) {
        return config.flatMap(loaded -> loaded.model().getString("id"));
    }

    @Test
    void startupUsesRememberedPathFirst() throws IOException {
        Path remembered = fileWith("remembered.vcfg", "remembered");
        Path detected = fileWith("detected.vcfg", "detected");
        ConfigRepository repository = repository(detected);
        preferences.setPathOverride(PREFERENCE_KEY, remembered);

        assertEquals(Optional.of("remembered"), idOf(repository.loadOnStartup()));
    }

    @Test
    void startupFallsBackToAutoDetection() throws IOException {
        Path detected = fileWith("detected.vcfg", "detected");
        ConfigRepository repository = repository(detected);

        assertEquals(Optional.of("detected"), idOf(repository.loadOnStartup()));
    }

    @Test
    void startupIgnoresRememberedPathWhenFileIsGone() throws IOException {
        Path detected = fileWith("detected.vcfg", "detected");
        ConfigRepository repository = repository(detected);
        preferences.setPathOverride(PREFERENCE_KEY, tempDir.resolve("missing.vcfg"));

        assertEquals(Optional.of("detected"), idOf(repository.loadOnStartup()));
    }

    @Test
    void startupReturnsEmptyWhenNothingIsFound() {
        assertTrue(repository(null).loadOnStartup().isEmpty());
    }

    @Test
    void redetectIgnoresRememberedPath() throws IOException {
        Path remembered = fileWith("remembered.vcfg", "remembered");
        ConfigRepository repository = repository(null);
        preferences.setPathOverride(PREFERENCE_KEY, remembered);

        // Startup would use the remembered path, re-detection must not
        assertTrue(repository.loadOnStartup().isPresent());
        assertTrue(repository.redetect().isEmpty());
    }

    @Test
    void loadAndRememberPersistsPath() throws IOException {
        Path file = fileWith("picked.vcfg", "picked");
        ConfigRepository repository = repository(null);

        assertEquals(Optional.of("picked"), idOf(Optional.of(repository.loadAndRemember(file))));
        assertEquals(Optional.of(file), preferences.getPathOverride(PREFERENCE_KEY));
    }

    @Test
    void readErrorsDegradeToEmptyOnStartup() {
        // Auto-detection points at a file that does not exist
        ConfigRepository repository = repository(tempDir.resolve("vanished.vcfg"));

        assertTrue(repository.loadOnStartup().isEmpty());
    }
}
