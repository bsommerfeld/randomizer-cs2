package de.bsommerfeld.randomizer.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.bsommerfeld.randomizer.steam.SteamLocator;
import de.bsommerfeld.randomizer.vdf.VdfObject;
import de.bsommerfeld.randomizer.vdf.VdfParseException;
import de.bsommerfeld.randomizer.vdf.VdfParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/** Orchestrates loading a CS2 config: locate the path, parse the VDF, render pretty JSON. */
public final class Cs2ConfigService {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private final AppPreferences preferences;
    private final SteamLocator steamLocator;

    public Cs2ConfigService(AppPreferences preferences, SteamLocator steamLocator) {
        this.preferences = preferences;
        this.steamLocator = steamLocator;
    }

    /**
     * Loads the config on app startup: the remembered path first (if the file still
     * exists), otherwise automatic detection. Errors degrade to empty.
     */
    public Optional<LoadedConfig> loadConfigOnStartup(ConfigKind kind) {
        Optional<Path> path = preferences.getConfigPathOverride(kind)
                .filter(Files::isRegularFile)
                .or(() -> autoDetect(kind));
        return loadQuietly(path);
    }

    /**
     * Re-runs detection from scratch (registry + libraryfolders.vdf / userdata scan),
     * ignoring any remembered path. Errors degrade to empty.
     */
    public Optional<LoadedConfig> redetectAndLoad(ConfigKind kind) {
        return loadQuietly(autoDetect(kind));
    }

    private Optional<LoadedConfig> loadQuietly(Optional<Path> path) {
        if (path.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(load(path.get()));
        } catch (IOException | VdfParseException e) {
            return Optional.empty();
        }
    }

    private Optional<Path> autoDetect(ConfigKind kind) {
        return switch (kind) {
            case DEFAULT -> steamLocator.findUserKeysDefaultConfig();
            case USER -> steamLocator.findUserKeysConfig();
        };
    }

    /** Loads the file and remembers its path for future startups. */
    public LoadedConfig loadAndRemember(ConfigKind kind, Path file) throws IOException {
        LoadedConfig config = load(file);
        preferences.setConfigPathOverride(kind, file);
        return config;
    }

    public LoadedConfig load(Path file) throws IOException {
        VdfObject config = VdfParser.parse(file);
        return new LoadedConfig(file, toDisplayJson(config));
    }

    /**
     * Pretty JSON for display. Backslashes are deliberately left unescaped
     * ({@code "\"} instead of {@code "\\"}) - more readable, but not strictly valid JSON.
     */
    private static String toDisplayJson(VdfObject config) {
        return GSON.toJson(config.asMap()).replace("\\\\", "\\");
    }

    public record LoadedConfig(Path source, String prettyJson) {
    }
}
