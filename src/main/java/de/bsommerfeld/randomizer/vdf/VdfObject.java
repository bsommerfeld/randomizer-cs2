package de.bsommerfeld.randomizer.vdf;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Node of a parsed VDF document: ordered key-value pairs whose values are
 * either {@link String} or nested {@link VdfObject} instances.
 */
public final class VdfObject {

    private final Map<String, Object> entries = new LinkedHashMap<>();

    void put(String key, Object value) {
        entries.put(key, value);
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
