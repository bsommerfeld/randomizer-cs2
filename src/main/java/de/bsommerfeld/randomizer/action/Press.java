package de.bsommerfeld.randomizer.action;

/**
 * How a key goes down for one action: held once for a random time within the range, or with
 * {@code clicks} set clicked again and again within that time, with a random pause from the pause
 * range between two clicks. Clicking is for weapons that fire once per click, holding the key would
 * give a single shot there no matter how long the action lasts.
 */
public record Press(boolean clicks, int minMillis, int maxMillis, int minPauseMillis, int maxPauseMillis) {

    static Press held(int minMillis, int maxMillis) {
        return new Press(false, minMillis, maxMillis, 0, 0);
    }

    /** A short press. 30 ms is long enough for CS2's input polling to see the key. */
    static Press tap() {
        return held(30, 60);
    }

    /** Five to eleven clicks a second, faster than any pistol fires. CS2 drops the clicks that come too early. */
    static Press spammed(int minMillis, int maxMillis) {
        return new Press(true, minMillis, maxMillis, 60, 140);
    }

    /**
     * Clicks at the pace of what the key sets off, for things so slow that a spam would be twenty
     * clicks per shot or per jump.
     */
    static Press paced(int minMillis, int maxMillis, int minPauseMillis, int maxPauseMillis) {
        return new Press(true, minMillis, maxMillis, minPauseMillis, maxPauseMillis);
    }
}
