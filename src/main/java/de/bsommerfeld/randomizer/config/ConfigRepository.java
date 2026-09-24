package de.bsommerfeld.randomizer.config;

import de.bsommerfeld.randomizer.config.keybinds.KeybindConfig;
import de.bsommerfeld.randomizer.vdf.VdfParseException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/** Loads the keybind config its {@link ConfigSource} describes and remembers a path the user picked. */
public final class ConfigRepository {

    private final ConfigSource source;
    private final AppPreferences preferences;

    public ConfigRepository(ConfigSource source, AppPreferences preferences) {
        this.source = source;
        this.preferences = preferences;
    }

    public ConfigSource source() {
        return source;
    }

    /**
     * Loads from the remembered path if that file still exists, otherwise from auto-detection.
     * Errors give empty.
     */
    public Optional<KeybindConfig> loadOnStartup() {
        return preferences.getPathOverride(source.preferenceKey())
                .filter(Files::isRegularFile)
                .or(source.autoDetect())
                .flatMap(this::loadQuietly);
    }

    /** Loads from auto-detection and ignores any remembered path. Errors give empty. */
    public Optional<KeybindConfig> redetect() {
        return source.autoDetect().get().flatMap(this::loadQuietly);
    }

    /** Loads the file and remembers its path for future startups. */
    public KeybindConfig loadAndRemember(Path file) throws IOException {
        KeybindConfig config = KeybindConfig.read(file);
        preferences.setPathOverride(source.preferenceKey(), file);
        return config;
    }

    private Optional<KeybindConfig> loadQuietly(Path file) {
        try {
            return Optional.of(KeybindConfig.read(file));
        } catch (IOException | VdfParseException e) {
            return Optional.empty();
        }
    }
}
