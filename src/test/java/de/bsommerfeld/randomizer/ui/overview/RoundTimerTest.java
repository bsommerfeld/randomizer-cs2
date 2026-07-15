package de.bsommerfeld.randomizer.ui.overview;

import com.cs2gsi.nodes.BombState;
import com.cs2gsi.nodes.GameMode;
import com.cs2gsi.nodes.Phase;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoundTimerTest {

    private long now = 1_000_000L;
    private final RoundTimer timer = new RoundTimer(() -> now);

    @Test
    void noTimerBeforeAnyRoundGoesLive() {
        assertTrue(timer.remaining().isEmpty());

        timer.update(Phase.Freezetime, BombState.Undefined, GameMode.Competitive);
        assertTrue(timer.remaining().isEmpty());
    }

    @Test
    void roundGoingLiveStartsCompetitiveCountdown() {
        timer.update(Phase.Live, BombState.Undefined, GameMode.Competitive);

        RoundTimer.Remaining remaining = timer.remaining().orElseThrow();
        assertEquals(115.0, remaining.seconds(), 1e-9);
        assertFalse(remaining.bomb());
    }

    @Test
    void wingmanRoundStartsAtNinetySeconds() {
        timer.update(Phase.Live, BombState.Undefined, GameMode.Scrimcomp2v2);

        assertEquals(90.0, timer.remaining().orElseThrow().seconds(), 1e-9);
    }

    @Test
    void countdownFollowsTheClock() {
        timer.update(Phase.Live, BombState.Undefined, GameMode.Competitive);

        now += 30_000;
        assertEquals(85.0, timer.remaining().orElseThrow().seconds(), 1e-9);
    }

    @Test
    void bombPlantTakesOverWithDetonationCountdown() {
        timer.update(Phase.Live, BombState.Undefined, GameMode.Competitive);
        now += 60_000;
        timer.update(Phase.Live, BombState.Planted, GameMode.Competitive);

        RoundTimer.Remaining remaining = timer.remaining().orElseThrow();
        assertEquals(40.0, remaining.seconds(), 1e-9);
        assertTrue(remaining.bomb());
    }

    @Test
    void repeatedPlantedStatesDoNotRestartTheBombTimer() {
        timer.update(Phase.Live, BombState.Planted, GameMode.Competitive);
        now += 10_000;
        timer.update(Phase.Live, BombState.Planted, GameMode.Competitive);

        assertEquals(30.0, timer.remaining().orElseThrow().seconds(), 1e-9);
    }

    @Test
    void timerStopsWhenRoundIsNoLongerLive() {
        timer.update(Phase.Live, BombState.Undefined, GameMode.Competitive);
        timer.update(Phase.Over, BombState.Undefined, GameMode.Competitive);

        assertTrue(timer.remaining().isEmpty());
    }

    @Test
    void nextRoundGoingLiveRestartsTheCountdown() {
        timer.update(Phase.Live, BombState.Undefined, GameMode.Competitive);
        timer.update(Phase.Over, BombState.Undefined, GameMode.Competitive);
        now += 5_000;
        timer.update(Phase.Live, BombState.Undefined, GameMode.Competitive);

        assertEquals(115.0, timer.remaining().orElseThrow().seconds(), 1e-9);
    }

    @Test
    void remainingNeverGoesNegative() {
        timer.update(Phase.Live, BombState.Undefined, GameMode.Competitive);

        now += 500_000;
        assertEquals(0.0, timer.remaining().orElseThrow().seconds(), 1e-9);
    }
}
