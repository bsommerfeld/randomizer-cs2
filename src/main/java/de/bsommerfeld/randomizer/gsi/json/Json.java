package de.bsommerfeld.randomizer.gsi.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;

/** The one pretty-printing {@link Gson}, so the GSI views and the keybind views print JSON alike. */
public final class Json {

    /** HTML escaping is off because CS2 payloads contain URLs and file paths. */
    private static final Gson PRETTY = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private Json() {
    }

    /** Pretty-prints already-serialized JSON; returns the input unchanged if it does not parse. */
    public static String prettify(String rawJson) {
        try {
            return PRETTY.toJson(JsonParser.parseString(rawJson));
        } catch (RuntimeException e) {
            return rawJson;
        }
    }

    /** Pretty-prints a JSON element, or a plain object such as nested maps. */
    public static String pretty(Object value) {
        return PRETTY.toJson(value);
    }
}
