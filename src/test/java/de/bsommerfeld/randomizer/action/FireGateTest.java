package de.bsommerfeld.randomizer.action;

import com.cs2gsi.GameState;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FireGateTest {

    private static final String LOCAL = "76561198000000001";

    @Test
    void openForTheLivingLocalPlayerInALiveRound() {
        assertEquals(Optional.empty(), closedBecause(true, state("playing", LOCAL, 100, "live", "live")));
        assertEquals(Optional.empty(), closedBecause(true, state("playing", LOCAL, 1, "over", "over")),
                "the seconds after the round count");
        assertEquals(Optional.empty(), closedBecause(true, state("playing", LOCAL, 100, "live", "warmup")));
    }

    @Test
    void closedWithoutGsiOrData() {
        assertClosed("GSI", closedBecause(false, state("playing", LOCAL, 100, "live", "live")));
        assertClosed("data", closedBecause(true, new GameState()));
    }

    @Test
    void closedOutsideOfPlay() {
        assertClosed("menu", closedBecause(true, state("menu", LOCAL, 100, "live", "live")));
        assertClosed("chat", closedBecause(true, state("textinput", LOCAL, 100, "live", "live")));
    }

    @Test
    void closedWhileDeadOrWatching() {
        assertClosed("dead", closedBecause(true, state("playing", LOCAL, 0, "live", "live")));
        assertClosed("watching", closedBecause(true, state("playing", "76561198000000002", 100, "live", "live")));
    }

    @Test
    void closedWhileTheGameIgnoresInput() {
        assertClosed("freezetime", closedBecause(true, state("playing", LOCAL, 100, "freezetime", "freezetime")));
        assertClosed("pause", closedBecause(true, state("playing", LOCAL, 100, "live", "paused")));
        assertClosed("timeout", closedBecause(true, state("playing", LOCAL, 100, "live", "timeout_ct")));
    }

    @Test
    void closedOnceCs2HasBeenSilentForTooLongWhateverTheLastStateSays() {
        GameState playing = state("playing", LOCAL, 100, "live", "live");

        assertEquals(Optional.empty(), FireGate.closedBecause(true, playing, FireGate.SILENT_AFTER),
                "two lost heartbeats are no silence yet");
        assertClosed("sent nothing", FireGate.closedBecause(true, playing, FireGate.SILENT_AFTER.plusMillis(1)));
        assertClosed("sent nothing", FireGate.closedBecause(true, state("menu", LOCAL, 100, "live", "live"),
                FireGate.SILENT_AFTER.plusMillis(1)));
    }

    @Test
    void aPlayerWhoCannotBeShownToBeTheLocalOneCountsAsWatched() {
        GameState withoutProvider = new GameState(JsonParser.parseString(
                "{\"player\":{\"steamid\":\"%s\",\"activity\":\"playing\",\"state\":{\"health\":100}}}"
                        .formatted(LOCAL)).getAsJsonObject());

        assertClosed("watching", closedBecause(true, withoutProvider));
    }

    /** The gate for a state that has only just come in. */
    private static Optional<String> closedBecause(boolean gsiRunning, GameState state) {
        return FireGate.closedBecause(gsiRunning, state, Duration.ZERO);
    }

    private static void assertClosed(String expectedWord, Optional<String> reason) {
        assertTrue(reason.isPresent() && reason.get().contains(expectedWord), "was: " + reason);
    }

    /** A payload as CS2 posts it, with {@link #LOCAL} as the account the game runs on. */
    private static GameState state(String activity, String playerSteamId, int health, String roundPhase,
                                   String countdownPhase) {
        return new GameState(JsonParser.parseString("""
                {"provider":{"steamid":"%s"},
                 "player":{"steamid":"%s","activity":"%s","state":{"health":%d}},
                 "round":{"phase":"%s"},
                 "phase_countdowns":{"phase":"%s"}}
                """.formatted(LOCAL, playerSteamId, activity, health, roundPhase, countdownPhase)).getAsJsonObject());
    }
}
