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
 * The app's settings in {@code %LOCALAPPDATA%\randomizer-cs2\app.properties}, or under user.home
 * without LOCALAPPDATA. The keys belong to whoever stores them. Every access reads the file anew,
 * so a hand edit applies without a restart.
 */
public final class AppPreferences {

    private final Path propertiesFile;

    public AppPreferences() {
        this(baseDir(System.getenv("LOCALAPPDATA"), System.getProperty("user.home")));
    }

    public AppPreferences(Path baseDir) {
        this.propertiesFile = baseDir.resolve("app.properties");
    }

    /** The app's data directory: under {@code localAppData} if set, else under {@code userHome}. Not guaranteed to exist yet. */
    static Path baseDir(String localAppData, String userHome) {
        String parent = localAppData == null || localAppData.isBlank() ? userHome : localAppData;
        return Path.of(parent).resolve("randomizer-cs2");
    }

    /**
     * The remembered path stored under {@code key}, or empty if there is none, it does not parse or
     * the file cannot be read. Empty is safe here, the caller falls back to auto-detection.
     */
    public Optional<Path> getPathOverride(String key) {
        try {
            String value = load().getProperty(key);
            return value == null || value.isBlank() ? Optional.empty() : Optional.of(Path.of(value));
        } catch (IOException | InvalidPathException e) {
            return Optional.empty();
        }
    }

    /** Remembers {@code path} under {@code key} for future startups, see {@link #setString}. */
    public void setPathOverride(String key, Path path) throws IOException {
        setString(key, path.toString());
    }

    /** The setting stored under {@code key}, or {@code fallback} if it is missing, blank or the file cannot be read. */
    public String getString(String key, String fallback) {
        try {
            String value = load().getProperty(key);
            return value == null || value.isBlank() ? fallback : value.trim();
        } catch (IOException e) {
            return fallback;
        }
    }

    /**
     * Stores {@code value} under {@code key}. Fails without writing when the existing file cannot be
     * read, because writing anyway would drop every other setting.
     */
    public void setString(String key, String value) throws IOException {
        Properties properties = load();
        properties.setProperty(key, value);
        Files.createDirectories(propertiesFile.getParent());
        try (OutputStream out = Files.newOutputStream(propertiesFile)) {
            properties.store(out, "Randomizer CS2");
        }
    }

    /** The stored properties, empty when the file does not exist yet. */
    private Properties load() throws IOException {
        Properties properties = new Properties();
        if (Files.isRegularFile(propertiesFile)) {
            try (InputStream in = Files.newInputStream(propertiesFile)) {
                properties.load(in);
            } catch (IllegalArgumentException malformed) { // e.g. a broken unicode escape in a hand-edited file
                throw new IOException(propertiesFile + " is damaged: " + malformed.getMessage(), malformed);
            }
        }
        return properties;
    }
}
