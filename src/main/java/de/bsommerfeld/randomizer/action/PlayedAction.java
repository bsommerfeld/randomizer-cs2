package de.bsommerfeld.randomizer.action;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * One entry of the randomizer's log: which action ran when, on which key and for how long, from the
 * first key down to the last key up.
 */
public record PlayedAction(LocalTime time, Action action, String key, int heldMillis) {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    /** The log line. The columns are padded, the log shows them in a monospaced font. */
    public String summary() {
        return String.format("%s  %-14s %-7s %d ms", TIME_FORMAT.format(time), action.name(), key, heldMillis);
    }
}
