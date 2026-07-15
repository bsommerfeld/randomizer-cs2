package de.bsommerfeld.randomizer.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;

/**
 * Persists app settings as a properties file at
 * {@code %LOCALAPPDATA%\randomizer-cs2\app.properties} (fallback: user.home).
 *
 * <p>Stores generic {@code key -> path} overrides; which keys exist is decided by the
 * {@link ConfigSource} implementations, so new configs need no change here.
 */
public final class AppPreferences {

    private final Path propertiesFile;

    public AppPreferences() {
        this(defaultBaseDir());
    }

    public AppPreferences(Path baseDir) {
        this.propertiesFile = baseDir.resolve("app.properties");
    }

    private static Path defaultBaseDir() {
        String localAppData = System.getenv("LOCALAPPDATA");
        Path base = localAppData == null || localAppData.isBlank()
                ? Path.of(System.getProperty("user.home"))
                : Path.of(localAppData);
        return base.resolve("randomizer-cs2");
    }

    /** The remembered path stored under {@code key}, or empty if none (or unparseable). */
    public Optional<Path> getPathOverride(String key) {
        String value = load().getProperty(key);
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Path.of(value));
        } catch (InvalidPathException e) {
            return Optional.empty();
        }
    }

    /** Remembers {@code path} under {@code key} for future startups. */
    public void setPathOverride(String key, Path path) throws IOException {
        Properties properties = load();
        properties.setProperty(key, path.toString());
        Files.createDirectories(propertiesFile.getParent());
        try (OutputStream out = Files.newOutputStream(propertiesFile)) {
            properties.store(out, "Randomizer CS2");
        }
    }

    private Properties load() {
        Properties properties = new Properties();
        if (Files.isRegularFile(propertiesFile)) {
            try (InputStream in = Files.newInputStream(propertiesFile)) {
                properties.load(in);
            } catch (IOException ignored) {
            }
        }
        return properties;
    }
}
