package de.bsommerfeld.randomizer.config;

/** The two CS2 keybind configs displayed by the app. */
public enum ConfigKind {

    /** Default keybinds: {@code game/csgo/cfg/user_keys_default.vcfg}. */
    DEFAULT("cs2.config.path", "user_keys_default.vcfg"),

    /** The user's custom keybinds: {@code userdata/<SteamID>/730/remote/cs2_user_keys.vcfg}. */
    USER("cs2.userconfig.path", "cs2_user_keys.vcfg");

    private final String preferenceKey;
    private final String fileName;

    ConfigKind(String preferenceKey, String fileName) {
        this.preferenceKey = preferenceKey;
        this.fileName = fileName;
    }

    String preferenceKey() {
        return preferenceKey;
    }

    /** The only file name that is valid for this config. */
    public String fileName() {
        return fileName;
    }
}
