package de.bsommerfeld.randomizer.vdf;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Node of a parsed VDF document: ordered key-value pairs whose values are
 * either {@link String} or nested {@link VdfObject} instances.
 *
 * <p>Read-only for callers: only the parser fills a node, {@link #mergedWith} builds new ones and
 * {@link #entries()} is an unmodifiable view.
 */
public final class VdfObject {

    private final Map<String, Object> entries = new LinkedHashMap<>();

    /** Insert used by the parser while it builds the document. */
    void put(String key, Object value) {
        entries.put(key, value);
    }

    /**
     * This node with {@code override} laid on top: where both sides hold a nested object the two
     * merge key by key, every other entry of {@code override} replaces this one's. Keys keep this
     * node's order, keys only {@code override} has are appended. Neither input changes. Nested nodes
     * only one side has are shared, nothing outside the parser can change a node.
     */
    public VdfObject mergedWith(VdfObject override) {
        VdfObject merged = new VdfObject();
        merged.entries.putAll(entries);
        override.entries.forEach((key, value) -> merged.entries.put(key,
                value instanceof VdfObject theirs && entries.get(key) instanceof VdfObject ours
                        ? ours.mergedWith(theirs)
                        : value));
        return merged;
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

    /** Recursively converts the node into plain maps, e.g. for JSON serialization. */
    public Map<String, Object> asMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        entries.forEach((key, value) -> map.put(key, value instanceof VdfObject nested ? nested.asMap() : value));
        return map;
    }
}
