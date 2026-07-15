package de.bsommerfeld.randomizer.config.keybinds;

import de.bsommerfeld.randomizer.config.ConfigSource;
import de.bsommerfeld.randomizer.steam.SteamLocator;

import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Function;

/** The two CS2 keybind configs displayed by the app. */
public enum ConfigKind implements ConfigSource {

    /** Default keybinds: {@code game/csgo/cfg/user_keys_default.vcfg}. */
    DEFAULT("cs2.config.path", "user_keys_default.vcfg", SteamLocator::findUserKeysDefaultConfig),

    /** The user's custom keybinds: {@code userdata/<SteamID>/730/remote/cs2_user_keys.vcfg}. */
    USER("cs2.userconfig.path", "cs2_user_keys.vcfg", SteamLocator::findUserKeysConfig);

    private final String preferenceKey;
    private final String fileName;
    private final Function<SteamLocator, Optional<Path>> detection;

    ConfigKind(String preferenceKey, String fileName, Function<SteamLocator, Optional<Path>> detection) {
        this.preferenceKey = preferenceKey;
        this.fileName = fileName;
        this.detection = detection;
    }

    @Override
    public String preferenceKey() {
        return preferenceKey;
    }

    @Override
    public String fileName() {
        return fileName;
    }

    @Override
    public Optional<Path> autoDetect(SteamLocator steamLocator) {
        return detection.apply(steamLocator);
    }
}
