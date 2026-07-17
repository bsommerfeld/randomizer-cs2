package de.bsommerfeld.randomizer.config;

import de.bsommerfeld.randomizer.vdf.VdfObject;
import de.bsommerfeld.randomizer.vdf.VdfParser;
import de.bsommerfeld.randomizer.vdf.VdfWriter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Writes edited convar values back into a live CS2 config file. The file is re-read immediately
 * before saving so concurrent changes to unrelated entries are never lost; exactly the given keys
 * are replaced wherever they already live in the tree, keys the document lacks are appended next
 * to their siblings, and the result is written atomically (temp file + move). Config-type
 * agnostic: what is written is defined entirely by the value map, so other configs (e.g. keybinds)
 * can reuse this unchanged.
 */
public final class ConfigSaver {

    /**
     * Sets {@code values} into {@code target} and rewrites it; all other entries keep their
     * current on-disk state. The file must already exist - CS2 owns it, so a missing file is an
     * error, not something to create.
     */
    public void save(Path target, Map<String, String> values) throws IOException {
        VdfObject root = VdfParser.parse(target);
        applyValues(root, values);
        Path temp = target.resolveSibling(target.getFileName() + ".tmp");
        Files.writeString(temp, VdfWriter.toText(root), StandardCharsets.UTF_8);
        try {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Replaces every string entry of {@code root} (at any nesting) whose key appears in
     * {@code values}, then appends the keys the document lacks: into the first node that already
     * held one of them, else into the first nested {@code "convars"} object, else at the root.
     */
    static void applyValues(VdfObject root, Map<String, String> values) {
        Set<String> unplaced = new LinkedHashSet<>(values.keySet());
        VdfObject host = replaceExisting(root, values, unplaced);
        if (unplaced.isEmpty()) {
            return;
        }
        VdfObject destination = host != null ? host : findObjectNamed(root, "convars").orElse(root);
        for (String key : unplaced) {
            destination.set(key, values.get(key));
        }
    }

    /** Replaces matches in place; returns the first node (document order) that held a target key. */
    private static VdfObject replaceExisting(VdfObject node, Map<String, String> values, Set<String> unplaced) {
        VdfObject host = null;
        for (String key : List.copyOf(node.entries().keySet())) {
            Object value = node.entries().get(key);
            if (value instanceof VdfObject child) {
                VdfObject childHost = replaceExisting(child, values, unplaced);
                if (host == null) {
                    host = childHost;
                }
            } else if (values.containsKey(key)) {
                node.set(key, values.get(key));
                unplaced.remove(key);
                if (host == null) {
                    host = node;
                }
            }
        }
        return host;
    }

    private static Optional<VdfObject> findObjectNamed(VdfObject node, String name) {
        for (Map.Entry<String, Object> entry : node.entries().entrySet()) {
            if (entry.getValue() instanceof VdfObject child) {
                if (entry.getKey().equals(name)) {
                    return Optional.of(child);
                }
                Optional<VdfObject> nested = findObjectNamed(child, name);
                if (nested.isPresent()) {
                    return nested;
                }
            }
        }
        return Optional.empty();
    }
}
