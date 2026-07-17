package de.bsommerfeld.randomizer.vdf;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Node of a parsed VDF document: ordered key-value pairs whose values are
 * either {@link String} or nested {@link VdfObject} instances.
 *
 * <p>Mutable so it can serve as an editable working copy: {@link #set} and {@link #remove} change
 * entries, {@link #copy} forks an independent deep clone (e.g. to edit without touching the loaded
 * original). Reading via {@link #entries()} stays an unmodifiable view.
 */
public final class VdfObject {

    private final Map<String, Object> entries = new LinkedHashMap<>();

    /** Generic insert used by the parser; the public, type-safe entry points are the {@code set} overloads. */
    void put(String key, Object value) {
        entries.put(key, value);
    }

    /** Sets or replaces a string entry. */
    public void set(String key, String value) {
        entries.put(key, value);
    }

    /** Sets or replaces a nested-object entry. */
    public void set(String key, VdfObject value) {
        entries.put(key, value);
    }

    /** Removes the entry for {@code key} if present. */
    public void remove(String key) {
        entries.remove(key);
    }

    /** An independent deep copy: nested objects are cloned, so edits don't leak into the original. */
    public VdfObject copy() {
        VdfObject clone = new VdfObject();
        entries.forEach((key, value) ->
                clone.entries.put(key, value instanceof VdfObject nested ? nested.copy() : value));
        return clone;
    }

    public Optional<String> getString(String key) {
        return entries.get(key) instanceof String value ? Optional.of(value) : Optional.empty();
    }

    public Optional<VdfObject> getObject(String key) {
        return entries.get(key) instanceof VdfObject value ? Optional.of(value) : Optional.empty();
    }

    public Map<String, Object> entries() {
        return Collections.unmodifiableMap(entries);
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    /** Recursively converts the node into plain maps, e.g. for JSON serialization. */
    public Map<String, Object> asMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        entries.forEach((key, value) -> map.put(key, value instanceof VdfObject nested ? nested.asMap() : value));
        return map;
    }
}
