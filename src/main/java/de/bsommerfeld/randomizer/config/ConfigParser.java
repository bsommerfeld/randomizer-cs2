package de.bsommerfeld.randomizer.config;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Turns a config file into its domain model. One implementation per config type; the loading
 * pipeline ({@link ConfigRepository}) is generic over it.
 *
 * @param <T> the parsed model
 */
@FunctionalInterface
public interface ConfigParser<T> {

    /**
     * Parses {@code file}.
     *
     * @throws IOException                                       if the file cannot be read
     * @throws de.bsommerfeld.randomizer.vdf.VdfParseException   if the content is not valid VDF
     */
    T parse(Path file) throws IOException;
}
