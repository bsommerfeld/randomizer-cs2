package de.bsommerfeld.randomizer.gsi.state;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

/** Pure, recursive structural diff of two JSON objects. Stateless and side-effect free. */
public final class JsonDiff {

    private JsonDiff() {
    }

    /**
     * Collects every entry whose value differs between {@code previous} and {@code next}: new and
     * changed values are copied in with their new value, nested objects are diffed recursively, and
     * keys present only in {@code previous} are recorded as JSON null. Arrays are compared as a
     * whole - a changed array is reported wholesale rather than element by element.
     *
     * @return the changes, or {@code null} when nothing changed.
     */
    public static JsonObject diff(JsonObject previous, JsonObject next) {
        JsonObject changes = new JsonObject();

        for (var entry : next.entrySet()) {
            String key = entry.getKey();
            JsonElement newValue = entry.getValue();
            JsonElement oldValue = previous.get(key);

            if (oldValue == null) {
                changes.add(key, newValue); // newly appeared
            } else if (oldValue.isJsonObject() && newValue.isJsonObject()) {
                JsonObject nested = diff(oldValue.getAsJsonObject(), newValue.getAsJsonObject());
                if (nested != null) {
                    changes.add(key, nested);
                }
            } else if (!oldValue.equals(newValue)) {
                changes.add(key, newValue); // value changed
            }
        }

        for (var entry : previous.entrySet()) {
            if (!next.has(entry.getKey())) {
                changes.add(entry.getKey(), JsonNull.INSTANCE); // disappeared
            }
        }

        return changes.isEmpty() ? null : changes;
    }
}
