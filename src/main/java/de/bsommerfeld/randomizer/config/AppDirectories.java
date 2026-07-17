package de.bsommerfeld.randomizer.config;

import java.nio.file.Path;

/**
 * Resolves the app's data directory. Everything the app persists (preferences, config backups)
 * lives under {@code %LOCALAPPDATA%\randomizer-cs2} (fallback: {@code user.home}).
 */
public final class AppDirectories {

    private AppDirectories() {
    }

    /** The base data directory; not guaranteed to exist yet. */
    public static Path base() {
        String localAppData = System.getenv("LOCALAPPDATA");
        Path base = localAppData == null || localAppData.isBlank()
                ? Path.of(System.getProperty("user.home"))
                : Path.of(localAppData);
        return base.resolve("randomizer-cs2");
    }
}
