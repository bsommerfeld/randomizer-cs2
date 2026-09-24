package de.bsommerfeld.randomizer.config;

import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * One loadable CS2 config file.
 *
 * @param preferenceKey key under which a manually chosen path is remembered in {@link AppPreferences}
 * @param fileName      the only file name that is valid for this config
 * @param autoDetect    locates the file without a remembered path, never throws
 */
public record ConfigSource(String preferenceKey, String fileName, Supplier<Optional<Path>> autoDetect) {
}
