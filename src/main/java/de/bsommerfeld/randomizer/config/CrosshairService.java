package de.bsommerfeld.randomizer.config;

import de.bsommerfeld.randomizer.steam.SteamLocator;
import de.bsommerfeld.randomizer.vdf.VdfObject;
import de.bsommerfeld.randomizer.vdf.VdfParseException;
import de.bsommerfeld.randomizer.vdf.VdfParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Loads the crosshair settings from {@code cs2_user_convars.vcfg}: locate the file (remembered path
 * or auto-detection), parse the VDF and pull out every {@code cl_crosshair*} convar. Mirrors
 * {@link Cs2ConfigService}; errors degrade to empty.
 */
public final class CrosshairService {

    /** The convar file name — the only file valid for the manual picker. */
    public static final String FILE_NAME = "cs2_user_convars.vcfg";

    private final AppPreferences preferences;
    private final SteamLocator steamLocator;

    public CrosshairService(AppPreferences preferences, SteamLocator steamLocator) {
        this.preferences = preferences;
        this.steamLocator = steamLocator;
    }

    /** Remembered path first (if it still exists), otherwise auto-detection. */
    public Optional<Crosshair> loadOnStartup() {
        Optional<Path> path = preferences.getCrosshairPathOverride()
                .filter(Files::isRegularFile)
                .or(steamLocator::findUserConvarsConfig);
        return loadQuietly(path);
    }

    /** Re-runs detection from scratch, ignoring any remembered path. */
    public Optional<Crosshair> redetect() {
        return loadQuietly(steamLocator.findUserConvarsConfig());
    }

    /** Loads the file and remembers its path for future startups. */
    public Crosshair loadAndRemember(Path file) throws IOException {
        Crosshair crosshair = load(file);
        preferences.setCrosshairPathOverride(file);
        return crosshair;
    }

    public Crosshair load(Path file) throws IOException {
        VdfObject root = VdfParser.parse(file);
        Map<String, String> convars = new LinkedHashMap<>();
        collectCrosshairConvars(root, convars);
        return new Crosshair(file, convars, CrosshairSettings.fromConvars(convars));
    }

    private Optional<Crosshair> loadQuietly(Optional<Path> path) {
        if (path.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(load(path.get()));
        } catch (IOException | VdfParseException e) {
            return Optional.empty();
        }
    }

    /** Recursively collects every {@code cl_crosshair*} string entry, regardless of nesting. */
    static void collectCrosshairConvars(VdfObject node, Map<String, String> out) {
        node.entries().forEach((key, value) -> {
            if (value instanceof VdfObject child) {
                collectCrosshairConvars(child, out);
            } else if (value instanceof String text && key.startsWith("cl_crosshair")) {
                out.put(key, text);
            }
        });
    }

    /** A loaded crosshair: source file, the raw convars for display, and the parsed settings. */
    public record Crosshair(Path source, Map<String, String> convars, CrosshairSettings settings) {
    }
}
