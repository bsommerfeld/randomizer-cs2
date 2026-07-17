package de.bsommerfeld.randomizer.exec;

import java.util.Locale;
import java.util.Optional;

/**
 * Maps configurable exec-trigger key names to Windows key codes. Supported: the function keys
 * {@code f1}-{@code f24} and the letter keys {@code a}-{@code z}.
 *
 * <p>Each key carries both its virtual-key code and its PS/2 set-1 scancode: games like CS2 read
 * input via scancodes/raw input and ignore synthesized events that carry only a virtual key.
 * Letter scancodes address the physical US-QWERTY position; on German QWERTZ layouts only
 * {@code y}/{@code z} are swapped, every other letter sits on the same key.
 */
public final class ExecKeys {

    /** A resolved exec-trigger key: Windows virtual-key code plus hardware scancode. */
    public record Key(int virtualKey, int scanCode) {
    }

    private static final int VK_F1 = 0x70;
    private static final int VK_A = 0x41;

    /** PS/2 scan code set 1 for the letters a-z, in alphabetical order. */
    private static final int[] LETTER_SCAN_CODES = {
            0x1E, 0x30, 0x2E, 0x20, 0x12, 0x21, 0x22, 0x23, 0x17, 0x24, 0x25, 0x26, 0x32,
            0x31, 0x18, 0x19, 0x10, 0x13, 0x1F, 0x14, 0x16, 0x2F, 0x11, 0x2D, 0x15, 0x2C};

    private ExecKeys() {
    }

    /** The key codes for {@code name} ({@code f1}-{@code f24} or {@code a}-{@code z}), or empty. */
    public static Optional<Key> key(String name) {
        if (name == null) {
            return Optional.empty();
        }
        String normalized = name.trim().toLowerCase(Locale.ROOT);
        if (normalized.matches("[a-z]")) {
            int index = normalized.charAt(0) - 'a';
            return Optional.of(new Key(VK_A + index, LETTER_SCAN_CODES[index]));
        }
        if (normalized.matches("f([1-9]|1[0-9]|2[0-4])")) {
            int number = Integer.parseInt(normalized.substring(1));
            return Optional.of(new Key(VK_F1 + number - 1, functionScanCode(number)));
        }
        return Optional.empty();
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
