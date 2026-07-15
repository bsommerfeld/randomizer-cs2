package de.bsommerfeld.randomizer.config;

import de.bsommerfeld.randomizer.steam.SteamLocator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the generic loading pipeline with a fake {@link ConfigSource}: remembered path first,
 * auto-detection as fallback, quiet error degradation, and remembering manual picks.
 */
class ConfigRepositoryTest {

    private static final String PREFERENCE_KEY = "test.path";

    @TempDir
    Path tempDir;

    /** A source whose auto-detection yields exactly {@code detected} (or empty). */
    private record TestSource(Path detected) implements ConfigSource {

        @Override
        public String preferenceKey() {
            return PREFERENCE_KEY;
        }

        @Override
        public String fileName() {
            return "test.vcfg";
        }

        @Override
        public Optional<Path> autoDetect(SteamLocator steamLocator) {
            return Optional.ofNullable(detected);
        }
    }

    private AppPreferences preferences;

    private ConfigRepository<String> repository(Path detected) {
        preferences = new AppPreferences(tempDir.resolve("prefs"));
        return new ConfigRepository<>(new TestSource(detected), preferences,
                new SteamLocator((hive, keyPath, valueName) -> Optional.empty()),
                Files::readString);
    }

    private Path fileWith(String name, String content) throws IOException {
        Path file = tempDir.resolve(name);
        Files.writeString(file, content);
        return file;
    }

    @Test
    void startupUsesRememberedPathFirst() throws IOException {
        Path remembered = fileWith("remembered.vcfg", "remembered");
        Path detected = fileWith("detected.vcfg", "detected");
        ConfigRepository<String> repository = repository(detected);
        preferences.setPathOverride(PREFERENCE_KEY, remembered);

        assertEquals(Optional.of("remembered"), repository.loadOnStartup());
    }

    @Test
    void startupFallsBackToAutoDetection() throws IOException {
        Path detected = fileWith("detected.vcfg", "detected");
        ConfigRepository<String> repository = repository(detected);

        assertEquals(Optional.of("detected"), repository.loadOnStartup());
    }

    @Test
    void startupIgnoresRememberedPathWhenFileIsGone() throws IOException {
        Path detected = fileWith("detected.vcfg", "detected");
        ConfigRepository<String> repository = repository(detected);
        preferences.setPathOverride(PREFERENCE_KEY, tempDir.resolve("missing.vcfg"));

        assertEquals(Optional.of("detected"), repository.loadOnStartup());
    }

    @Test
    void startupReturnsEmptyWhenNothingIsFound() {
        assertTrue(repository(null).loadOnStartup().isEmpty());
    }

    @Test
    void redetectIgnoresRememberedPath() throws IOException {
        Path remembered = fileWith("remembered.vcfg", "remembered");
        ConfigRepository<String> repository = repository(null);
        preferences.setPathOverride(PREFERENCE_KEY, remembered);

        // Startup would use the remembered path, re-detection must not
        assertTrue(repository.loadOnStartup().isPresent());
        assertTrue(repository.redetect().isEmpty());
    }

    @Test
    void loadAndRememberPersistsPath() throws IOException {
        Path file = fileWith("picked.vcfg", "picked");
        ConfigRepository<String> repository = repository(null);

        assertEquals("picked", repository.loadAndRemember(file));
        assertEquals(Optional.of(file), preferences.getPathOverride(PREFERENCE_KEY));
    }

    @Test
    void readErrorsDegradeToEmptyOnStartup() {
        // Auto-detection points at a file that does not exist → the read fails quietly.
        ConfigRepository<String> repository = repository(tempDir.resolve("vanished.vcfg"));

        assertTrue(repository.loadOnStartup().isEmpty());
    }
}
