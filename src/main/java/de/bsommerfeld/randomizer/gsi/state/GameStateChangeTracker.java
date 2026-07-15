package de.bsommerfeld.randomizer.gsi.state;

import com.google.gson.JsonObject;
import de.bsommerfeld.randomizer.gsi.json.Json;

import java.util.Set;

/**
 * Remembers the last game state and reports, on each new state, only the values that changed
 * relative to it - as pretty JSON. CS2's own {@code previously}/{@code added} delta blocks are
 * dropped so only actual state changes surface.
 *
 * <p>Not thread-safe: drive it from a single thread (the GSI listener dispatches on one).
 */
public final class GameStateChangeTracker {

    /** Top-level keys CS2 uses for its own delta bookkeeping - noise for our own diff. */
    private static final Set<String> META_KEYS = Set.of("previously", "added");

    private JsonObject previousState;

    /**
     * Feeds the next raw game-state JSON and returns the pretty-printed changes since the previous
     * one, or {@code null} for the first state (nothing to compare against), when the JSON is
     * unusable, or when nothing changed.
     */
    public String track(String rawStateJson) {
        JsonObject next = Json.asObject(rawStateJson);
        if (next == null) {
            return null;
        }
        META_KEYS.forEach(next::remove);

        JsonObject previous = previousState;
        previousState = next;
        if (previous == null) {
            return null; // first state: nothing to compare against
        }

        JsonObject changes = JsonDiff.diff(previous, next);
        return changes == null ? null : Json.pretty(changes);
    }
}
