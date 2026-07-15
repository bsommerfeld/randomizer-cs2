package de.bsommerfeld.randomizer.gsi.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Shared JSON plumbing: a single pretty-printing {@link Gson} plus small parse/format helpers.
 * Kept in one place so every part of the GSI pipeline serializes identically.
 */
public final class Json {

    /** Pretty-printing; HTML escaping is off because CS2 payloads contain URLs and file paths. */
    public static final Gson PRETTY = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

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

    /** Parses {@code rawJson} into an object, or {@code null} if it is absent or not an object. */
    public static JsonObject asObject(String rawJson) {
        try {
            JsonElement element = JsonParser.parseString(rawJson);
            if (element.isJsonObject()) {
                return element.getAsJsonObject();
            }
        } catch (RuntimeException ignored) {
        }
        return null;
    }

    /** Pretty-prints a JSON element. */
    public static String pretty(JsonElement element) {
        return PRETTY.toJson(element);
    }
}
