package de.bsommerfeld.randomizer.ui.overview;

import com.cs2gsi.nodes.BombState;
import com.cs2gsi.nodes.GameMode;
import com.cs2gsi.nodes.Phase;

import java.util.Optional;
import java.util.function.LongSupplier;

/**
 * Derives the round/bomb countdown from game-state transitions. CS2 does not send
 * {@code phase_ends_in} in normal matches, so the timer is state-machine based: the round going
 * live starts a 1:55 countdown (Wingman: 1:30), a bomb plant starts a 40s countdown (it takes
 * over, as the round is still live), and the timer stops as soon as the round is no longer live
 * (bomb exploded/defused, round over, back in freezetime).
 *
 * <p>Pure logic, no UI - the clock is injectable for tests.
 */
final class RoundTimer {

    /** Standard competitive round time (1:55). */
    private static final long COMPETITIVE_ROUND_SECONDS = 115;
    /** Wingman (2v2) round time (1:30). */
    private static final long WINGMAN_ROUND_SECONDS = 90;
    /** C4 detonation time. */
    private static final long BOMB_SECONDS = 40;

    /** A running countdown: seconds left, and whether it counts down the bomb rather than the round. */
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

    /** Feeds the next game state's round phase, bomb state and game mode into the state machine. */
    void update(Phase roundPhase, BombState bombState, GameMode mode) {
        if (bombState == BombState.Planted && prevBomb != BombState.Planted) {
            start(BOMB_SECONDS, true); // bomb just planted → detonation countdown
        } else if (roundPhase == Phase.Live && prevPhase != Phase.Live) {
            long roundSeconds = mode == GameMode.Scrimcomp2v2 ? WINGMAN_ROUND_SECONDS : COMPETITIVE_ROUND_SECONDS;
            start(roundSeconds, false); // round just went live
        }
        if (roundPhase != Phase.Live) {
            deadlineMillis = -1; // round no longer live → stop
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

    private void start(long seconds, boolean bomb) {
        deadlineMillis = clock.getAsLong() + seconds * 1000L;
        bombTimer = bomb;
    }
}
