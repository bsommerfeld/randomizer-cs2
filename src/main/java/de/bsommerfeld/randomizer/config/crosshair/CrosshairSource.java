package de.bsommerfeld.randomizer.config.crosshair;

import de.bsommerfeld.randomizer.config.ConfigSource;
import de.bsommerfeld.randomizer.steam.SteamLocator;

import java.nio.file.Path;
import java.util.Optional;

/** The convar file holding the crosshair settings: {@code cs2_user_convars.vcfg}. */
public enum CrosshairSource implements ConfigSource {

    INSTANCE;

    @Override
    public String preferenceKey() {
        return "cs2.convars.path";
    }

    @Override
    public String fileName() {
        return "cs2_user_convars.vcfg";
    }

    @Override
    public Optional<Path> autoDetect(SteamLocator steamLocator) {
        return steamLocator.findUserConvarsConfig();
    }
}
