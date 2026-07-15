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
 */
public final class AppPreferences {

    private static final String CROSSHAIR_PATH_KEY = "cs2.convars.path";

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

    public Optional<Path> getConfigPathOverride(ConfigKind kind) {
        return getPathOverride(kind.preferenceKey());
    }

    public void setConfigPathOverride(ConfigKind kind, Path path) throws IOException {
        setPathOverride(kind.preferenceKey(), path);
    }

    /** Remembered path to {@code cs2_user_convars.vcfg} (the crosshair settings). */
    public Optional<Path> getCrosshairPathOverride() {
        return getPathOverride(CROSSHAIR_PATH_KEY);
    }

    public void setCrosshairPathOverride(Path path) throws IOException {
        setPathOverride(CROSSHAIR_PATH_KEY, path);
    }

    private Optional<Path> getPathOverride(String key) {
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

    private void setPathOverride(String key, Path path) throws IOException {
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
