package de.bsommerfeld.randomizer.input;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Maps the key names CS2 writes into its keybind files ({@code w}, {@code SPACE}, {@code MOUSE1}) to
 * what {@link GameInput} can press. Names outside this table, the mouse wheel among them, give empty.
 *
 * <p>Keyboard keys carry their PS/2 set-1 scancode, see {@link JnaGameInput}. A letter's scancode is
 * its physical US-QWERTY position. On a German QWERTZ layout only {@code y} and {@code z} land on
 * another key.
 */
public final class Keys {

    /** A pressable key: a keyboard scancode, or with {@code mouse} set the button number 1-5 of MOUSE1-MOUSE5. */
    public record Key(boolean mouse, int code) {
    }

    /** PS/2 scan code set 1 for the letters a-z, in alphabetical order. */
    private static final int[] LETTER_SCAN_CODES = {
            0x1E, 0x30, 0x2E, 0x20, 0x12, 0x21, 0x22, 0x23, 0x17, 0x24, 0x25, 0x26, 0x32,
            0x31, 0x18, 0x19, 0x10, 0x13, 0x1F, 0x14, 0x16, 0x2F, 0x11, 0x2D, 0x15, 0x2C};

    /** The left-hand modifiers. The right-hand ones need the extended flag, which nothing sends yet. */
    private static final Map<String, Integer> NAMED_SCAN_CODES = Map.of(
            "space", 0x39, "ctrl", 0x1D, "shift", 0x2A, "alt", 0x38, "tab", 0x0F);

    private Keys() {
    }

    /** The key CS2 calls {@code name}, case ignored, or empty when this table does not know it. */
    public static Optional<Key> key(String name) {
        if (name == null) {
            return Optional.empty();
        }
        String normalized = name.trim().toLowerCase(Locale.ROOT);
        if (normalized.matches("[a-z]")) {
            return keyboard(LETTER_SCAN_CODES[normalized.charAt(0) - 'a']);
        }
        if (normalized.matches("[0-9]")) {
            return keyboard(digitScanCode(normalized.charAt(0) - '0'));
        }
        if (normalized.matches("f([1-9]|1[0-9]|2[0-4])")) {
            return keyboard(functionScanCode(Integer.parseInt(normalized.substring(1))));
        }
        if (normalized.matches("mouse[1-5]")) {
            return Optional.of(new Key(true, normalized.charAt(5) - '0'));
        }
        return Optional.ofNullable(NAMED_SCAN_CODES.get(normalized)).flatMap(Keys::keyboard);
    }

    private static Optional<Key> keyboard(int scanCode) {
        return Optional.of(new Key(false, scanCode));
    }

    /** PS/2 scan code set 1: the digit row runs 1-9 and ends with 0. */
    private static int digitScanCode(int digit) {
        return digit == 0 ? 0x0B : 0x01 + digit;
    }

    /** PS/2 scan code set 1: F1-F10 are contiguous, F11/F12 and F24 sit apart. */
    private static int functionScanCode(int number) {
        if (number <= 10) {
            return 0x3B + number - 1;
        }
        return switch (number) {
            case 11 -> 0x57;
            case 12 -> 0x58;
            case 24 -> 0x76;
            default -> 0x64 + number - 13; // F13-F23
        };
    }
}
