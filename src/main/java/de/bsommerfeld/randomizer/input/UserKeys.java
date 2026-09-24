package de.bsommerfeld.randomizer.input;

import de.bsommerfeld.randomizer.input.Keys.Key;

import java.util.function.Consumer;

/**
 * Which keys the user holds with their own hands, as opposed to the ones the randomizer presses.
 * Watching means reading every key the user touches, so the randomizer watches only while it runs.
 * An interface so the randomizer's logic can be tested without real devices.
 */
public interface UserKeys {

    void start();

    void stop();

    /** Whether the user holds {@code key} right now. False for every key while not started. */
    boolean holds(Key key);

    /**
     * Registers the one listener that hears each key the user lets go of, at the moment it happens.
     * It runs on the watching thread, so it has to return quickly.
     */
    void onRelease(Consumer<Key> listener);
}
