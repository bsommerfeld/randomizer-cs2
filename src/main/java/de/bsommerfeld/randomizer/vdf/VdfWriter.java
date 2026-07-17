package de.bsommerfeld.randomizer.vdf;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Serializes a {@link VdfObject} back to Valve's text KeyValues format - the inverse of
 * {@link VdfParser}. Uses tab indentation and quotes every key and value, matching how CS2 writes
 * its own config files.
 *
 * <p>Escaping is symmetric with the parser: a backslash is written as {@code \\} (the parser turns
 * {@code \\} back into a single {@code \}), so any document survives a parse → write → parse cycle
 * unchanged in meaning. Two things the parser drops are consequently not restored: {@code //}
 * comments and platform conditionals like {@code [$WIN32]}. For machine-generated CS2 user configs
 * that is irrelevant; the byte-exact app-data backup
 * ({@link de.bsommerfeld.randomizer.config.crosshair.CrosshairBackup}) is the rollback safety net.
 */
public final class VdfWriter {

    private VdfWriter() {
    }

    /** Writes {@code root} to {@code file} as UTF-8 VDF text. */
    public static void write(VdfObject root, Path file) throws IOException {
        Files.writeString(file, toText(root), StandardCharsets.UTF_8);
    }

    /** Renders {@code root} as VDF text. */
    public static String toText(VdfObject root) {
        StringBuilder sb = new StringBuilder();
        writePairs(root, sb, 0);
        return sb.toString();
    }

    private static void writePairs(VdfObject node, StringBuilder sb, int depth) {
        String indent = "\t".repeat(depth);
        node.entries().forEach((key, value) -> {
            if (value instanceof VdfObject child) {
                sb.append(indent).append('"').append(escape(key)).append("\"\n");
                sb.append(indent).append("{\n");
                writePairs(child, sb, depth + 1);
                sb.append(indent).append("}\n");
            } else {
                sb.append(indent)
                        .append('"').append(escape(key)).append("\"\t\t")
                        .append('"').append(escape((String) value)).append("\"\n");
            }
        });
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\");
    }
}
