package de.bsommerfeld.randomizer.exec;

/**
 * Abstraction over the native CS2 game window, so the exec-trigger logic can be tested without a
 * running game (mirrors the {@link de.bsommerfeld.randomizer.steam.WindowsRegistry} pattern).
 */
public interface Cs2Window {

    enum PressResult {
        /** The window was focused and the key was sent. */
        PRESSED,
        /** No CS2 window exists (game not running, or not on Windows). */
        WINDOW_NOT_FOUND,
        /** The window exists but could not be brought to the foreground. */
        FOCUS_DENIED
    }

    /** Brings the CS2 window to the foreground and presses the given key. */
    PressResult pressKey(ExecKeys.Key key);
}
