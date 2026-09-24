package de.bsommerfeld.randomizer.input;

import de.bsommerfeld.randomizer.input.Keys.Key;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Which keys and mouse buttons the user holds with their own hands right now, fed with the raw
 * input events of real devices. Written by the {@link RawInputWatcher}'s thread, read by the
 * randomizer's. Only events that arrive while CS2 is the foreground window count, the rest is dropped.
 *
 * <p>ponytail: a key up that never arrives leaves its key marked as held. That takes a switch to
 * the secure desktop (lock screen, UAC prompt) with the key down. The mark clears the next time the
 * user taps that key. Clear the whole set on such a switch if it ever shows up in play.
 */
public final class PhysicalKeys {

    /** RAWKEYBOARD.Flags: set on key up. */
    private static final int RI_KEY_BREAK = 0x01;
    /** RAWKEYBOARD.Flags: the extended prefix. Right Ctrl is E0 1D, left Ctrl plain 1D. */
    private static final int RI_KEY_E0 = 0x02;

    private final Set<Key> held = ConcurrentHashMap.newKeySet();
    private volatile Consumer<Key> onRelease = key -> { };
    private final BooleanSupplier cs2Foreground;

    PhysicalKeys(BooleanSupplier cs2Foreground) {
        this.cs2Foreground = cs2Foreground;
    }

    public boolean holds(Key key) {
        return held.contains(key);
    }

    /** The one listener for every key up, called on the thread that feeds the events in. */
    void onRelease(Consumer<Key> listener) {
        this.onRelease = listener;
    }

    /** Forgets every key. What was held before the watching began, or after it ended, is unknown. */
    void clear() {
        held.clear();
    }

    /** A keyboard event: the PS/2 set-1 make code, the same code {@link Keys} sends, and the raw flags. */
    void keyboard(int makeCode, int flags) {
        // Right Ctrl, right Alt, the arrows and the fake shifts Windows adds around them share their make
        // code with a key Keys sends. None of them is that key. Taken for it, the randomizer would leave
        // its key up out while the user holds right Ctrl, and left Ctrl would stay down.
        if ((flags & RI_KEY_E0) != 0) {
            return;
        }
        set(new Key(false, makeCode), (flags & RI_KEY_BREAK) == 0);
    }

    /**
     * A mouse event with its RAWMOUSE.usButtonFlags: two bits per button, down then up, from button 1
     * at the lowest bit to button 5. A plain mouse move carries no bit and changes nothing.
     */
    void mouse(int buttonFlags) {
        for (int button = 1; button <= 5; button++) {
            int down = 1 << (2 * (button - 1));
            if ((buttonFlags & down) != 0) {
                set(new Key(true, button), true);
            }
            if ((buttonFlags & (down << 1)) != 0) {
                set(new Key(true, button), false);
            }
        }
    }

    private void set(Key key, boolean down) {
        if (!cs2Foreground.getAsBoolean()) {
            // What the user types into another window is none of the randomizer's business. The key ups
            // out there never count, so a key held while leaving the game would stay marked forever.
            held.clear();
            return;
        }
        if (!down) {
            held.remove(key);
            onRelease.accept(key); // also for a key that went down before the watching began, the game saw this key up too
        } else {
            held.add(key);
        }
    }
}
