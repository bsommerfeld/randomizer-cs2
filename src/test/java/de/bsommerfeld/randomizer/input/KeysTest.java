package de.bsommerfeld.randomizer.input;

import de.bsommerfeld.randomizer.input.Keys.Key;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KeysTest {

    @Test
    void lettersDigitsAndFunctionKeysCarryTheirScanCode() {
        assertEquals(Optional.of(new Key(false, 0x11)), Keys.key("w"));
        assertEquals(Optional.of(new Key(false, 0x22)), Keys.key("g"));
        assertEquals(Optional.of(new Key(false, 0x02)), Keys.key("1"));
        assertEquals(Optional.of(new Key(false, 0x0B)), Keys.key("0"));
        assertEquals(Optional.of(new Key(false, 0x3B)), Keys.key("F1"));
        assertEquals(Optional.of(new Key(false, 0x57)), Keys.key("F11"));
    }

    @Test
    void namedKeysAreFoundInAnyCase() {
        assertEquals(Optional.of(new Key(false, 0x39)), Keys.key("SPACE"));
        assertEquals(Optional.of(new Key(false, 0x39)), Keys.key(" space "));
        assertEquals(Optional.of(new Key(false, 0x1D)), Keys.key("CTRL"));
        assertEquals(Optional.of(new Key(false, 0x2A)), Keys.key("Shift"));
        assertEquals(Keys.key("w"), Keys.key("W"));
    }

    @Test
    void mouseButtonsCarryTheirNumber() {
        assertEquals(Optional.of(new Key(true, 1)), Keys.key("MOUSE1"));
        assertEquals(Optional.of(new Key(true, 5)), Keys.key("mouse5"));
    }

    @Test
    void whatCannotBeSentIsEmpty() {
        assertEquals(Optional.empty(), Keys.key("MWHEELUP"));
        assertEquals(Optional.empty(), Keys.key("MOUSE6"));
        assertEquals(Optional.empty(), Keys.key("F25"));
        assertEquals(Optional.empty(), Keys.key("`"));
        assertEquals(Optional.empty(), Keys.key(""));
        assertEquals(Optional.empty(), Keys.key(null));
    }
}
