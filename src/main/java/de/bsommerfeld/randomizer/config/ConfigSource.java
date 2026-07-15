package de.bsommerfeld.randomizer.config;

import de.bsommerfeld.randomizer.steam.SteamLocator;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Describes one loadable CS2 config file: where its remembered path is stored, which file name is
 * valid for it and how it is auto-detected. New configs are added by implementing this interface -
 * the loading pipeline ({@link ConfigRepository}) and the preference store stay untouched
 * (open/closed principle).
 */
public interface ConfigSource {

    /** Key under which a manually chosen path is remembered in {@link AppPreferences}. */
    String preferenceKey();

    /** The only file name that is valid for this config. */
    String fileName();

    /** Locates the file automatically (registry, Steam libraries, userdata scan); never throws. */
    Optional<Path> autoDetect(SteamLocator steamLocator);
}
