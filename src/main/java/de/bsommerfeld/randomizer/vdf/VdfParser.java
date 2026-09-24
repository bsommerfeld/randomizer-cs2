package de.bsommerfeld.randomizer.vdf;


import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Parser for Valve's text-based KeyValues format (VDF/VCFG/ACF).
 * Supports quoted and unquoted tokens, nested {@code { }} blocks and
 * {@code //} comments. Platform conditionals like {@code [$WIN32]} are
 * ignored. Of two equal keys the last one wins.
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

    /** The whole document: pairs up to the end of the input, a closing brace has no block to close. */
    private VdfObject parseRoot() {
        VdfObject root = parsePairs();
        if (!atEnd()) {
            throw new VdfParseException("Unexpected '}' at the top level (position " + pos + ")");
        }
        return root;
    }

    /** A nested block, entered right after its '{': pairs up to the '}' that must close it. */
    private VdfObject parseBlock() {
        VdfObject block = parsePairs();
        if (atEnd()) {
            throw new VdfParseException("Unexpected end of file: a closing '}' is missing");
        }
        pos++; // the closing brace
        return block;
    }

    /** Reads pairs until the input ends or a '}' comes up; the brace is left for the caller. */
    private VdfObject parsePairs() {
        VdfObject target = new VdfObject();
        while (hasToken()) {
            String key = readToken();
            target.put(key, readValue(key));
        }
        return target;
    }

    /** Skips ahead and tells whether a token follows, as opposed to a '}' or the end of the input. */
    private boolean hasToken() {
        skipIgnorable();
        return !atEnd() && src.charAt(pos) != '}';
    }

    /** The value after {@code key}: a nested block or a plain token. */
    private Object readValue(String key) {
        if (!hasToken()) {
            throw new VdfParseException("The key \"" + key + "\" has no value");
        }
        if (src.charAt(pos) == '{') {
            pos++;
            return parseBlock();
        }
        return readToken();
    }

    private boolean atEnd() {
        return pos >= src.length();
    }

    /** Skips whitespace, // comments and conditionals like [$WIN32]. */
    private void skipIgnorable() {
        while (!atEnd()) {
            char c = src.charAt(pos);
            if (Character.isWhitespace(c)) {
                pos++;
            } else if (src.startsWith("//", pos)) {
                skipTo('\n');
            } else if (c == '[') {
                skipTo(']');
                pos = Math.min(pos + 1, src.length()); // the closing bracket, if there is one
            } else {
                return;
            }
        }
    }

    /** Advances to the next {@code stop} character, or to the end of the input if there is none. */
    private void skipTo(char stop) {
        int found = src.indexOf(stop, pos);
        pos = found < 0 ? src.length() : found;
    }

    private String readToken() {
        return src.charAt(pos) == '"' ? readQuoted() : readUnquoted();
    }

    private String readUnquoted() {
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
        throw new VdfParseException("A string has no closing quote");
    }
}
