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
        String value = load().getProperty(kind.preferenceKey());
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Path.of(value));
        } catch (InvalidPathException e) {
            return Optional.empty();
        }
    }

    public void setConfigPathOverride(ConfigKind kind, Path path) throws IOException {
        Properties properties = load();
        properties.setProperty(kind.preferenceKey(), path.toString());
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
