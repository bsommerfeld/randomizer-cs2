package de.bsommerfeld.randomizer.config;

import de.bsommerfeld.randomizer.vdf.VdfObject;
import de.bsommerfeld.randomizer.vdf.VdfParser;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Verifies that saved convar values actually ended up in a config file: re-reads the file and
 * reports every expected entry the on-disk document does not hold. A key counts as set only if it
 * appears at least once and every occurrence (at any nesting) carries the expected value -
 * {@link ConfigSaver} replaces all occurrences, so a single stale one means the save did not take.
 * Config-type agnostic, like the saver.
 */
public final class ConfigVerifier {

    /**
     * Returns the entries of {@code expected} that {@code target} does not hold (missing key, or an
     * occurrence with a different value), in {@code expected}'s order; empty when everything is set.
     */
    public Map<String, String> missingValues(Path target, Map<String, String> expected) throws IOException {
        VdfObject root = VdfParser.parse(target);
        Map<String, String> missing = new LinkedHashMap<>();
        expected.forEach((key, value) -> {
            if (countMatching(root, key, value) < 1) {
                missing.put(key, value);
            }
        });
        return missing;
    }

    /**
     * Occurrences of {@code key} as a string entry whose value equals {@code value}; a single
     * occurrence with a different value poisons the result to {@code -1}.
     */
    private static int countMatching(VdfObject node, String key, String value) {
        int matches = 0;
        for (Map.Entry<String, Object> entry : node.entries().entrySet()) {
            if (entry.getValue() instanceof VdfObject child) {
                int nested = countMatching(child, key, value);
                if (nested < 0) {
                    return -1;
                }
                matches += nested;
            } else if (entry.getKey().equals(key)) {
                if (!value.equals(entry.getValue())) {
                    return -1;
                }
                matches++;
            }
        }
        return matches;
    }
}
