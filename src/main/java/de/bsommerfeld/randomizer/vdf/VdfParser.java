package de.bsommerfeld.randomizer.vdf;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Parser for Valve's text-based KeyValues format (VDF/VCFG/ACF).
 * Supports quoted and unquoted tokens, nested {@code { }} blocks and
 * {@code //} comments. Platform conditionals like {@code [$WIN32]} are
 * ignored. Duplicate keys: the last one wins.
 *
 * <p>Escaping: inside quoted strings only {@code \\} is treated as an escape
 * sequence (yielding a single backslash); any other backslash stays literal.
 * This covers both styles found in the wild: {@code libraryfolders.vdf} escapes
 * paths ({@code "D:\\SteamLibrary"}), while CS2 keybind files write backslashes
 * raw (e.g. the key {@code "\"} for the backslash key).
 */
public final class VdfParser {

    private static final String BOM = String.valueOf((char) 0xFEFF);

    private final String src;
    private int pos;

    private VdfParser(String src) {
        this.src = src.startsWith(BOM) ? src.substring(1) : src;
    }

    public static VdfObject parse(Path file) throws IOException {
        return parse(Files.readString(file, StandardCharsets.UTF_8));
    }

    public static VdfObject parse(String text) {
        return new VdfParser(text).parseRoot();
    }

    private VdfObject parseRoot() {
        VdfObject root = new VdfObject();
        parsePairs(root, true);
        return root;
    }

    private void parsePairs(VdfObject target, boolean isRoot) {
        while (true) {
            skipIgnorable();
            if (pos >= src.length()) {
                if (!isRoot) {
                    throw new VdfParseException("Unerwartetes Dateiende: schließendes '}' fehlt");
                }
                return;
            }
            char c = src.charAt(pos);
            if (c == '}') {
                if (isRoot) {
                    throw new VdfParseException("Unerwartetes '}' auf oberster Ebene (Position " + pos + ")");
                }
                pos++;
                return;
            }
            String key = readToken();
            skipIgnorable();
            if (pos >= src.length() || src.charAt(pos) == '}') {
                throw new VdfParseException("Wert für Schlüssel \"" + key + "\" fehlt");
            }
            if (src.charAt(pos) == '{') {
                pos++;
                VdfObject child = new VdfObject();
                parsePairs(child, false);
                target.put(key, child);
            } else {
                target.put(key, readToken());
            }
        }
    }

    /** Skips whitespace, // comments and conditionals like [$WIN32]. */
    private void skipIgnorable() {
        while (pos < src.length()) {
            char c = src.charAt(pos);
            if (Character.isWhitespace(c)) {
                pos++;
            } else if (c == '/' && pos + 1 < src.length() && src.charAt(pos + 1) == '/') {
                while (pos < src.length() && src.charAt(pos) != '\n') {
                    pos++;
                }
            } else if (c == '[') {
                while (pos < src.length() && src.charAt(pos) != ']') {
                    pos++;
                }
                if (pos < src.length()) {
                    pos++;
                }
            } else {
                return;
            }
        }
    }

    private String readToken() {
        if (src.charAt(pos) == '"') {
            return readQuoted();
        }
        int start = pos;
        while (pos < src.length()) {
            char c = src.charAt(pos);
            if (Character.isWhitespace(c) || c == '{' || c == '}' || c == '"') {
                break;
            }
            pos++;
        }
        return src.substring(start, pos);
    }

    private String readQuoted() {
        pos++;
        StringBuilder sb = new StringBuilder();
        while (pos < src.length()) {
            char c = src.charAt(pos);
            if (c == '"') {
                pos++;
                return sb.toString();
            }
            if (c == '\\' && pos + 1 < src.length() && src.charAt(pos + 1) == '\\') {
                sb.append('\\');
                pos += 2;
                continue;
            }
            sb.append(c);
            pos++;
        }
        throw new VdfParseException("String ohne schließendes Anführungszeichen");
    }
}
