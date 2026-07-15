package de.bsommerfeld.randomizer.config;

import de.bsommerfeld.randomizer.steam.SteamLocator;
import de.bsommerfeld.randomizer.vdf.VdfParseException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * The one loading pipeline for every CS2 config: resolve the path (remembered path first, otherwise
 * auto-detection), parse the file via the {@link ConfigParser} and optionally remember a manually
 * chosen path. What is loaded (keybinds, crosshair, …) is entirely defined by the injected
 * {@link ConfigSource} and parser - adding a new config type does not change this class.
 *
 * @param <T> the parsed config model
 */
public final class ConfigRepository<T> {

    private final ConfigSource source;
    private final AppPreferences preferences;
    private final SteamLocator steamLocator;
    private final ConfigParser<T> parser;

    public ConfigRepository(ConfigSource source, AppPreferences preferences,
                            SteamLocator steamLocator, ConfigParser<T> parser) {
        this.source = source;
        this.preferences = preferences;
        this.steamLocator = steamLocator;
        this.parser = parser;
    }

    public ConfigSource source() {
        return source;
    }

    /**
     * Loads on app startup: the remembered path first (if the file still exists), otherwise
     * auto-detection. Errors degrade to empty.
     */
    public Optional<T> loadOnStartup() {
        Optional<Path> path = preferences.getPathOverride(source.preferenceKey())
                .filter(Files::isRegularFile)
                .or(() -> source.autoDetect(steamLocator));
        return loadQuietly(path);
    }

    /** Re-runs detection from scratch, ignoring any remembered path. Errors degrade to empty. */
    public Optional<T> redetect() {
        return loadQuietly(source.autoDetect(steamLocator));
    }

    /** Loads the file and remembers its path for future startups. */
    public T loadAndRemember(Path file) throws IOException {
        T config = load(file);
        preferences.setPathOverride(source.preferenceKey(), file);
        return config;
    }

    public T load(Path file) throws IOException {
        return parser.parse(file);
    }

    private Optional<T> loadQuietly(Optional<Path> path) {
        if (path.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(load(path.get()));
        } catch (IOException | VdfParseException e) {
            return Optional.empty();
        }
    }
}
