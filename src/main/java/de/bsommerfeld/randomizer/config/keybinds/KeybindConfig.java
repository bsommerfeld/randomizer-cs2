package de.bsommerfeld.randomizer.config.keybinds;

import de.bsommerfeld.randomizer.config.ConfigSource;
import de.bsommerfeld.randomizer.gsi.json.Json;
import de.bsommerfeld.randomizer.steam.SteamLocator;
import de.bsommerfeld.randomizer.vdf.VdfObject;
import de.bsommerfeld.randomizer.vdf.VdfParser;

import java.io.IOException;
import java.nio.file.Path;

/** A loaded keybind config: its source file and the parsed {@link VdfObject} model. */
public record KeybindConfig(Path source, VdfObject model) {

    /** The default keybinds, {@code game/csgo/cfg/user_keys_default.vcfg}. */
    public static ConfigSource defaults(SteamLocator steamLocator) {
        return new ConfigSource("cs2.config.path", "user_keys_default.vcfg", steamLocator::findUserKeysDefaultConfig);
    }

    /** The user's custom keybinds, {@code userdata/<SteamID>/730/remote/cs2_user_keys.vcfg}. */
    public static ConfigSource custom(SteamLocator steamLocator) {
        return new ConfigSource("cs2.userconfig.path", "cs2_user_keys.vcfg", steamLocator::findUserKeysConfig);
    }

    /**
     * Parses a keybind {@code .vcfg}.
     *
     * @throws de.bsommerfeld.randomizer.vdf.VdfParseException if the content is not valid VDF
     */
    public static KeybindConfig read(Path file) throws IOException {
        return new KeybindConfig(file, VdfParser.parse(file));
    }

    /** This config as pretty JSON for display. */
    public String prettyJson() {
        return displayJson(model);
    }

    /**
     * Any keybind model as pretty JSON for display, for example two configs merged. Backslashes stay
     * unescaped ({@code "\"} instead of {@code "\\"}). That reads better but is not valid JSON.
     */
    public static String displayJson(VdfObject model) {
        return Json.pretty(model.asMap()).replace("\\\\", "\\");
    }
}
