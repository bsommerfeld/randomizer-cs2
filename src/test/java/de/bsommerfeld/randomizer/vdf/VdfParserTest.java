package de.bsommerfeld.randomizer.vdf;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VdfParserTest {

    @Test
    void parsesNestedBlocksAndPreservesOrder() {
        VdfObject root = VdfParser.parse("""
                "config"
                {
                    "bindings"
                    {
                        "w" "+forward"
                        "a" "+left"
                    }
                }
                """);

        VdfObject bind = root.getObject("config").orElseThrow().getObject("bindings").orElseThrow();
        assertEquals("+forward", bind.getString("w").orElseThrow());
        assertEquals("+left", bind.getString("a").orElseThrow());
        assertEquals(List.of("w", "a"), List.copyOf(bind.entries().keySet()));
    }

    @Test
    void unescapesBackslashesInValues() {
        VdfObject root = VdfParser.parse("\"path\" \"D:\\\\SteamLibrary\"");

        assertEquals("D:\\SteamLibrary", root.getString("path").orElseThrow());
    }

    @Test
    void keepsSingleBackslashesLiteral() {
        // CS2 keybind files write backslashes raw, e.g. "a\tb" stays literal
        VdfObject root = VdfParser.parse("\"key\" \"a\\tb\"");

        assertEquals("a\\tb", root.getString("key").orElseThrow());
    }

    @Test
    void parsesUnescapedBackslashKeyFromCs2UserKeys() {
        // Real line from cs2_user_keys.vcfg: the key is a single backslash
        VdfObject root = VdfParser.parse("\"bindings\"\n{\n\t\"\\\"\t\t\"toggleconsole\"\n}");

        VdfObject bindings = root.getObject("bindings").orElseThrow();
        assertEquals("toggleconsole", bindings.getString("\\").orElseThrow());
    }

    @Test
    void skipsComments() {
        VdfObject root = VdfParser.parse("""
                // Kommentar am Anfang
                "a" "1" // Kommentar am Zeilenende
                "b" "2"
                """);

        assertEquals("1", root.getString("a").orElseThrow());
        assertEquals("2", root.getString("b").orElseThrow());
    }

    @Test
    void duplicateKeysLastWins() {
        VdfObject root = VdfParser.parse("\"a\" \"1\"\n\"a\" \"2\"");

        assertEquals("2", root.getString("a").orElseThrow());
        assertEquals(1, root.entries().size());
    }

    @Test
    void stripsByteOrderMark() {
        VdfObject root = VdfParser.parse((char) 0xFEFF + "\"a\" \"b\"");

        assertEquals("b", root.getString("a").orElseThrow());
    }

    @Test
    void supportsUnquotedTokens() {
        VdfObject root = VdfParser.parse("config\n{\n\tkey value\n}");

        assertEquals("value", root.getObject("config").orElseThrow().getString("key").orElseThrow());
    }

    @Test
    void supportsKeysWithSpaces() {
        VdfObject root = VdfParser.parse("\"key with spaces\" \"some value\"");

        assertEquals("some value", root.getString("key with spaces").orElseThrow());
    }

    @Test
    void skipsPlatformConditionals() {
        VdfObject root = VdfParser.parse("\"a\" \"1\" [$WIN32]\n\"b\" \"2\"");

        assertEquals("1", root.getString("a").orElseThrow());
        assertEquals("2", root.getString("b").orElseThrow());
    }

    @Test
    void missingClosingBraceThrows() {
        assertThrows(VdfParseException.class, () -> VdfParser.parse("\"a\" {"));
    }

    @Test
    void missingValueThrows() {
        assertThrows(VdfParseException.class, () -> VdfParser.parse("\"a\" { \"b\" }"));
    }

    @Test
    void unexpectedClosingBraceAtTopLevelThrows() {
        assertThrows(VdfParseException.class, () -> VdfParser.parse("}"));
    }

    @Test
    void unterminatedStringThrows() {
        assertThrows(VdfParseException.class, () -> VdfParser.parse("\"a\" \"unterminated"));
    }

    @Test
    void parsesFixtureFile() throws Exception {
        Path fixture = Path.of(getClass().getResource("/fixtures/user_keys_default.vcfg").toURI());

        VdfObject root = VdfParser.parse(fixture);

        VdfObject bind = root.getObject("config").orElseThrow().getObject("bindings").orElseThrow();
        assertEquals("+forward", bind.getString("w").orElseThrow());
        assertTrue(bind.entries().size() >= 10);
    }
}
