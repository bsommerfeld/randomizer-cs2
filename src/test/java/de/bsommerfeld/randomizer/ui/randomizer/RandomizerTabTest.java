package de.bsommerfeld.randomizer.ui.randomizer;

import de.bsommerfeld.randomizer.action.Action;
import de.bsommerfeld.randomizer.action.BoundKeys;
import de.bsommerfeld.randomizer.vdf.VdfParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The decisions behind the randomizer tab, checked without starting JavaFX. */
class RandomizerTabTest {

    private static final BoundKeys KEYS = BoundKeys.of(List.of(
            VdfParser.parse("\"config\" { \"bindings\" { \"SPACE\" \"+jump\" \"MWHEELUP\" \"+attack\" } }")));

    @Test
    void keyHintSaysWhichKeyAnActionPressesOrWhyItCannot() {
        assertEquals("SPACE", RandomizerTabController.keyHintText(action("Jump", "+jump"), KEYS));
        assertEquals("not bound", RandomizerTabController.keyHintText(action("Drop weapon", "drop"), KEYS));
        assertEquals("Key not supported: MWHEELUP",
                RandomizerTabController.keyHintText(action("Shoot", "+attack"), KEYS));
    }

    @Test
    void storedSecondsSurviveAHandEditedFile() {
        assertEquals(12, RandomizerTabController.clampedSeconds("12", 5, 1, 599));
        assertEquals(599, RandomizerTabController.clampedSeconds("100000", 5, 1, 599));
        assertEquals(1, RandomizerTabController.clampedSeconds("-3", 5, 1, 599));
        assertEquals(5, RandomizerTabController.clampedSeconds("bald", 5, 1, 599));
        assertEquals(5, RandomizerTabController.clampedSeconds("", 5, 1, 599));
    }

    private static Action action(String name, String command) {
        return new Action(name, command, 30, 60);
    }
}
