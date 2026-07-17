package de.bsommerfeld.randomizer.vdf;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VdfWriterTest {

    @Test
    void writesQuotedKeysValuesAndNestedBlocks() {
        VdfObject root = new VdfObject();
        VdfObject bindings = new VdfObject();
        bindings.set("w", "+forward");
        root.set("bindings", bindings);

        String text = VdfWriter.toText(root);

        assertTrue(text.contains("\"bindings\""), "nested key is quoted");
        assertTrue(text.contains("{"), "nested block is opened");
        assertTrue(text.contains("\"w\"\t\t\"+forward\""), "leaf is a quoted key/value pair");
    }

    @Test
    void roundTripsThroughTheParserUnchanged() {
        String source = """
                "UserConvars"
                {
                    "convars"
                    {
                        "cl_crosshairstyle" "4"
                        "cl_crosshairsize" "0.5"
                        "cl_crosshaircolor" "5"
                    }
                    "sensitivity" "1.5"
                }
                """;
        VdfObject original = VdfParser.parse(source);

        VdfObject reparsed = VdfParser.parse(VdfWriter.toText(original));

        assertEquals(original.asMap(), reparsed.asMap(), "parse → write → parse must preserve the tree");
    }

    @Test
    void backslashSurvivesTheRoundTrip() {
        // libraryfolders-style escaped path: parser stores a single backslash, writer must re-escape it.
        VdfObject original = VdfParser.parse("\"libraryfolders\"\n{\n\"1\" \"D:\\\\SteamLibrary\"\n}");

        VdfObject reparsed = VdfParser.parse(VdfWriter.toText(original));

        assertEquals("D:\\SteamLibrary",
                reparsed.getObject("libraryfolders").orElseThrow().getString("1").orElseThrow());
    }

    @Test
    void reflectsEditsMadeToTheModel() {
        VdfObject root = VdfParser.parse("\"convars\"\n{\n\"cl_crosshairsize\" \"5\"\n}");
        VdfObject convars = root.getObject("convars").orElseThrow();
        convars.set("cl_crosshairsize", "3");
        convars.set("cl_crosshairdot", "1");
        convars.remove("does_not_exist");

        VdfObject reparsed = VdfParser.parse(VdfWriter.toText(root));
        VdfObject reparsedConvars = reparsed.getObject("convars").orElseThrow();

        assertEquals("3", reparsedConvars.getString("cl_crosshairsize").orElseThrow());
        assertEquals("1", reparsedConvars.getString("cl_crosshairdot").orElseThrow());
    }
}
