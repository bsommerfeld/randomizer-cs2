package de.bsommerfeld.randomizer.input;

import de.bsommerfeld.randomizer.input.Keys.Key;

/**
 * Sends key presses to whatever window has the focus. An interface so the randomizer's logic can be
 * tested without touching the real keyboard.
 */
public interface GameInput {

    /** False when the key down did not go out. Windows drops input into a program that runs with higher rights. */
    boolean press(Key key);

    void release(Key key);

    /** Whether input sent now would land in CS2. */
    boolean isCs2Foreground();
}
