package de.bsommerfeld.randomizer.input;

import de.bsommerfeld.randomizer.input.Keys.Key;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhysicalKeysTest {

    private static final Key MOUSE1 = Keys.key("MOUSE1").orElseThrow();
    private static final Key MOUSE2 = Keys.key("MOUSE2").orElseThrow();
    private static final Key MOUSE5 = Keys.key("MOUSE5").orElseThrow();
    private static final Key W = Keys.key("w").orElseThrow();

    private boolean cs2InFront = true;
    private final PhysicalKeys keys = new PhysicalKeys(() -> cs2InFront);

    @Test
    void whatTheUserPressesOutsideCs2IsDroppedAndLeavingTheGameForgetsTheHeldKeys() {
        List<Key> released = new ArrayList<>();
        keys.onRelease(released::add);
        keys.keyboard(0x11, 0); // w down in the game

        cs2InFront = false;
        keys.mouse(0x0001);
        keys.mouse(0x0002);
        keys.keyboard(0x11, 1); // w comes up in another window

        assertFalse(keys.holds(W), "its key up was never seen, the mark must not stay");
        assertFalse(keys.holds(MOUSE1));
        assertEquals(List.of(), released, "a key up outside the game must not make the randomizer press anything");
    }

    @Test
    void aMouseButtonIsHeldFromItsDownFlagToItsUpFlag() {
        keys.mouse(0x0001); // RI_MOUSE_BUTTON_1_DOWN
        assertTrue(keys.holds(MOUSE1));
        assertFalse(keys.holds(MOUSE2));

        keys.mouse(0x0000); // a mouse move in between
        assertTrue(keys.holds(MOUSE1));

        keys.mouse(0x0002); // RI_MOUSE_BUTTON_1_UP
        assertFalse(keys.holds(MOUSE1));
    }

    @Test
    void theListenerHearsEveryKeyUpEvenOneWhoseKeyDownCameBeforeTheWatching() {
        List<Key> released = new ArrayList<>();
        keys.onRelease(released::add);

        keys.mouse(0x0001);
        keys.mouse(0x0002);
        keys.keyboard(0x11, 1); // w comes up and never went down here

        assertEquals(List.of(MOUSE1, W), released);
    }

    @Test
    void oneEventCanCarrySeveralButtons() {
        keys.mouse(0x0004 | 0x0100); // button 2 down and button 5 down
        assertTrue(keys.holds(MOUSE2));
        assertTrue(keys.holds(MOUSE5));

        keys.mouse(0x0008 | 0x0200);
        assertFalse(keys.holds(MOUSE2));
        assertFalse(keys.holds(MOUSE5));
    }

    @Test
    void aKeyboardKeyIsHeldUntilItsBreakFlag() {
        keys.keyboard(0x11, 0); // w down
        keys.keyboard(0x11, 0); // typematic repeat
        assertTrue(keys.holds(W));

        keys.keyboard(0x11, 1); // RI_KEY_BREAK
        assertFalse(keys.holds(W));
    }

    @Test
    void rightCtrlIsNotLeftCtrl() {
        keys.keyboard(0x1D, 0x02); // right Ctrl down, RI_KEY_E0

        assertFalse(keys.holds(Keys.key("CTRL").orElseThrow()), "CTRL in the keybinds is the left one");
    }
}
