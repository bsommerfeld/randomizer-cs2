package de.bsommerfeld.randomizer.config.keybinds;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.bsommerfeld.randomizer.config.ConfigParser;
import de.bsommerfeld.randomizer.vdf.VdfObject;
import de.bsommerfeld.randomizer.vdf.VdfParser;

import java.io.IOException;
import java.nio.file.Path;

/** Parses a keybind {@code .vcfg} and renders it as pretty JSON for display. */
public final class KeybindConfigParser implements ConfigParser<KeybindConfig> {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    @Override
    public KeybindConfig parse(Path file) throws IOException {
        VdfObject config = VdfParser.parse(file);
        return new KeybindConfig(file, toDisplayJson(config));
    }

    /**
     * Pretty JSON for display. Backslashes are deliberately left unescaped
     * ({@code "\"} instead of {@code "\\"}) - more readable, but not strictly valid JSON.
     */
    private static String toDisplayJson(VdfObject config) {
        return GSON.toJson(config.asMap()).replace("\\\\", "\\");
    }
}
