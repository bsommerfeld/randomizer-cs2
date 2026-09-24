package de.bsommerfeld.randomizer.ui.overview;

import com.cs2gsi.nodes.BombState;
import com.cs2gsi.nodes.GameMode;
import com.cs2gsi.nodes.Phase;

import java.util.Optional;
import java.util.function.LongSupplier;

/**
 * Counts the round and the bomb down from game-state changes, because CS2 does not send
 * {@code phase_ends_in} in a normal match. The round going live starts the round time. A bomb
 * plant switches to the bomb timer. The timer stops once the round is no longer live.
 *
 * <p>Both times are the GSI library's defaults per mode. A mode without them, like Deathmatch, gets
 * no timer. Casual hostage maps run 120 s, the timer shows the 135 s of the defusal maps there.
 */
final class RoundTimer {

    /** Seconds left, and whether they count down the bomb rather than the round. */
    record Remaining(double seconds, boolean bomb) {
    }

    private final LongSupplier clock;

    /** Epoch millis when the active countdown reaches zero, or negative when no timer runs. */
    private long deadlineMillis = -1;
    private boolean bombTimer;
    private Phase prevPhase = Phase.Undefined;
    private BombState prevBomb = BombState.Undefined;

    RoundTimer() {
        this(System::currentTimeMillis);
    }

    RoundTimer(LongSupplier clock) {
        this.clock = clock;
    }

    void update(Phase roundPhase, BombState bombState, GameMode mode) {
        boolean bombJustPlanted = bombState == BombState.Planted && prevBomb != BombState.Planted;
        boolean roundJustWentLive = roundPhase == Phase.Live && prevPhase != Phase.Live;
        if (bombJustPlanted) {
            start(mode.bombSeconds, true);
        } else if (roundJustWentLive) {
            start(mode.roundSeconds, false);
        }
        if (roundPhase != Phase.Live) {
            stop();
        }
        prevPhase = roundPhase;
        prevBomb = bombState;
    }

    /** The running countdown, or empty when no timer is active. Never negative. */
    Optional<Remaining> remaining() {
        if (deadlineMillis < 0) {
            return Optional.empty();
        }
        double seconds = Math.max(0, (deadlineMillis - clock.getAsLong()) / 1000.0);
        return Optional.of(new Remaining(seconds, bombTimer));
    }

    /** 0 is the library's "no fixed time", then no timer runs. */
    private void start(int seconds, boolean bomb) {
        deadlineMillis = seconds > 0 ? clock.getAsLong() + seconds * 1000L : -1;
        bombTimer = bomb;
    }

    private void stop() {
        deadlineMillis = -1;
    }
}
