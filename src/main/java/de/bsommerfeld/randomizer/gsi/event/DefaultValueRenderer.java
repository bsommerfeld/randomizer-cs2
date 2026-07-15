package de.bsommerfeld.randomizer.gsi.event;

import com.google.gson.JsonParser;
import de.bsommerfeld.randomizer.gsi.json.Json;

/**
 * Fallback renderer for any value without a more specific {@link ValueRenderer}: pretty-prints raw
 * JSON objects, expands node bracket notation across lines, and otherwise uses the value's own
 * {@code toString()}. {@link #supports(Object)} always returns {@code true}, so this is used as the
 * registry's last-resort renderer.
 */
public final class DefaultValueRenderer implements ValueRenderer {

    @Override
    public boolean supports(Object value) {
        return true;
    }

    @Override
    public String render(Object value) {
        if (value == null) {
            return "null";
        }
        String text = String.valueOf(value);
        if (text.startsWith("{")) {
            // Raw JSON (e.g. from Node.parsedData) - pretty-print it when possible
            try {
                return Json.pretty(JsonParser.parseString(text));
            } catch (RuntimeException ignored) {
            }
        }
        if (text.contains("[")) {
            // Node values render as "[SteamID: ..., State: [...]]" - indent the nesting
            return indentBrackets(text);
        }
        return text;
    }

    private static String indentBrackets(String raw) {
        StringBuilder sb = new StringBuilder();
        int indent = 0;
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            switch (c) {
                case '[' -> {
                    if (i + 1 < raw.length() && raw.charAt(i + 1) == ']') {
                        sb.append("[]");
                        i++;
                    } else {
                        indent++;
                        sb.append('[').append(System.lineSeparator()).append("  ".repeat(indent));
                    }
                }
                case ']' -> {
                    indent = Math.max(0, indent - 1);
                    sb.append(System.lineSeparator()).append("  ".repeat(indent)).append(']');
                }
                case ',' -> {
                    sb.append(',').append(System.lineSeparator()).append("  ".repeat(indent));
                    if (i + 1 < raw.length() && raw.charAt(i + 1) == ' ') {
                        i++;
                    }
                }
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }
}
