package de.bsommerfeld.randomizer.action;

import de.bsommerfeld.randomizer.vdf.VdfParser;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BoundKeysTest {

    @Test
    void takesTheFirstKeyThatCanBeSent() {
        BoundKeys keys = BoundKeys.of(List.of(
                VdfParser.parse("\"config\" { \"bindings\" { \"MWHEELUP\" \"+jump\" \"SPACE\" \"+jump\" \"n\" \"+jump\" } }")));

        assertEquals(List.of("MWHEELUP", "SPACE", "N"), keys.keysFor("+jump"));
        assertEquals(Optional.of("SPACE"), keys.pressableKeyFor("+jump"));
    }

    @Test
    void laterConfigReplacesTheSameKey() {
        BoundKeys keys = BoundKeys.of(List.of(
                VdfParser.parse("\"config\" { \"bindings\" { \"g\" \"drop\" \"MOUSE3\" \"player_ping\" \"q\" \"lastinv\" } }"),
                VdfParser.parse("\"config\" { \"bindings\" { \"G\" \"+jump\" \"MOUSE3\" \"<unbound>\" } }")));

        assertEquals(Optional.empty(), keys.pressableKeyFor("drop"), "g moved to +jump");
        assertEquals(Optional.of("G"), keys.pressableKeyFor("+jump"));
        assertEquals(List.of(), keys.keysFor("player_ping"), "<unbound> took the default away");
        assertEquals(Optional.of("Q"), keys.pressableKeyFor("lastinv"), "untouched defaults stay");
    }

    @Test
    void readsACustomFileWithoutTheConfigNode() {
        BoundKeys keys = BoundKeys.of(List.of(VdfParser.parse("\"bindings\" { \"x\" \"drop\" }")));

        assertEquals(Optional.of("X"), keys.pressableKeyFor("drop"));
    }

    @Test
    void aCommandOnlyOnTheWheelHasKeysButNoneToPress() {
        BoundKeys keys = BoundKeys.of(List.of(
                VdfParser.parse("\"config\" { \"bindings\" { \"MWHEELDOWN\" \"+jump\" \"b\" \"+jump; +duck\" } }")));

        assertEquals(List.of("MWHEELDOWN"), keys.keysFor("+jump"));
        assertEquals(Optional.empty(), keys.pressableKeyFor("+jump"));
        assertEquals(List.of(), keys.keysFor("+duck"), "a multi-command bind is no bind of its parts");
    }
}
