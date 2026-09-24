package de.bsommerfeld.randomizer.gsi.event;

import com.cs2gsi.nodes.Player;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventValuesTest {

    @Test
    void rendersAPlayerAsCompactIdentity() {
        Player player = new Player(
                JsonParser.parseString("{\"name\":\"bustolio\",\"team\":\"CT\"}").getAsJsonObject(), "111");

        assertEquals("[Name: bustolio, SteamID: 111, Team: CT]", EventValues.render(player));
    }

    @Test
    void prettyPrintsRawJson() {
        String rendered = EventValues.render("{\"phase\":\"live\"}");

        assertTrue(rendered.contains("\"phase\": \"live\""), rendered);
        assertTrue(rendered.lines().count() > 1, "should span several lines: " + rendered);
    }

    @Test
    void spreadsBracketNotationOverLines() {
        String nl = System.lineSeparator();

        assertEquals("[" + nl + "  A: 1," + nl + "  B: []" + nl + "]", EventValues.render("[A: 1, B: []]"));
    }

    @Test
    void leavesPlainValuesAndNullReadable() {
        assertEquals("7", EventValues.render(7));
        assertEquals("null", EventValues.render(null));
        assertEquals("{broken", EventValues.render("{broken"), "text that only looks like JSON stays as it is");
    }
}
