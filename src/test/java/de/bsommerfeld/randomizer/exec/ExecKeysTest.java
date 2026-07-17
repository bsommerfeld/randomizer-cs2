package de.bsommerfeld.randomizer.exec;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExecKeysTest {

    @Test
    void mapsFunctionKeysToVirtualKeyAndScanCode() {
        assertEquals(Optional.of(new ExecKeys.Key(0x70, 0x3B)), ExecKeys.key("f1"));
        assertEquals(Optional.of(new ExecKeys.Key(0x7A, 0x57)), ExecKeys.key("f11"));
        assertEquals(Optional.of(new ExecKeys.Key(0x7B, 0x58)), ExecKeys.key("f12"));
        assertEquals(Optional.of(new ExecKeys.Key(0x7C, 0x64)), ExecKeys.key("  f13 "));
        assertEquals(Optional.of(new ExecKeys.Key(0x86, 0x6E)), ExecKeys.key("f23"));
        assertEquals(Optional.of(new ExecKeys.Key(0x87, 0x76)), ExecKeys.key("F24"));
    }

    @Test
    void mapsLetterKeysToVirtualKeyAndScanCode() {
        assertEquals(Optional.of(new ExecKeys.Key(0x41, 0x1E)), ExecKeys.key("a"));
        assertEquals(Optional.of(new ExecKeys.Key(0x4C, 0x26)), ExecKeys.key("l"));
        assertEquals(Optional.of(new ExecKeys.Key(0x4C, 0x26)), ExecKeys.key(" L "));
        assertEquals(Optional.of(new ExecKeys.Key(0x5A, 0x2C)), ExecKeys.key("z"));
    }

    @Test
    void rejectsEverythingElse() {
        assertTrue(ExecKeys.key("f25").isEmpty());
        assertTrue(ExecKeys.key("f0").isEmpty());
        assertTrue(ExecKeys.key("ab").isEmpty());
        assertTrue(ExecKeys.key("1").isEmpty());
        assertTrue(ExecKeys.key("11").isEmpty());
        assertTrue(ExecKeys.key("").isEmpty());
        assertTrue(ExecKeys.key(null).isEmpty());
    }
}
