package de.bsommerfeld.randomizer.gsi.event;

import com.cs2gsi.nodes.Player;
import com.google.gson.JsonParser;
import de.bsommerfeld.randomizer.gsi.json.Json;

import java.util.Optional;

/** Renders the value of one event field as display text. */
final class EventValues {

    private EventValues() {
    }

    static String render(Object value) {
        return switch (value) {
            case null -> "null";
            case Player player -> identity(player);
            default -> text(String.valueOf(value));
        };
    }

    /**
     * A player as name, Steam ID and team. On "X changed" events the full player state is noise,
     * the change itself is in the event's own fields.
     */
    private static String identity(Player player) {
        boolean hasSteamId = player.steamId != null && !player.steamId.isBlank();
        return "[Name: " + player.name
                + (hasSteamId ? ", SteamID: " + player.steamId : "")
                + ", Team: " + player.team + "]";
    }

    /** Raw JSON pretty-printed, node bracket notation spread over lines, anything else as it is. */
    private static String text(String text) {
        return prettyJson(text).orElseGet(() -> text.contains("[") ? indentBrackets(text) : text);
    }

    /** Raw JSON (e.g. from Node.parsedData) pretty-printed, or empty if {@code text} is not JSON. */
    private static Optional<String> prettyJson(String text) {
        if (!text.startsWith("{")) {
            return Optional.empty();
        }
        try {
            return Optional.of(Json.pretty(JsonParser.parseString(text)));
        } catch (RuntimeException notJson) {
            return Optional.empty();
        }
    }

    /** Spreads a node value like "[SteamID: ..., State: [...]]" over one line per entry. */
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
