package de.bsommerfeld.randomizer.action;

import com.cs2gsi.GameState;
import com.cs2gsi.nodes.Phase;
import com.cs2gsi.nodes.PlayerActivity;
import de.bsommerfeld.randomizer.gsi.GsiService;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.function.BooleanSupplier;

/**
 * Decides from the game state and the window in front whether an action may fire. Without it the
 * randomizer would type into the chat, press keys in the menu or in another program and keep going
 * while the player is dead.
 */
public final class FireGate {

    /**
     * Phases in which the game ignores movement and shooting. Pauses and timeouts show up in the
     * phase countdowns, freezetime in both nodes, so both are checked.
     */
    private static final Set<Phase> FROZEN = Set.of(Phase.Freezetime, Phase.Paused, Phase.Timeout_T, Phase.Timeout_CT);

    /**
     * A state older than this says nothing about the game any more. CS2 repeats its state every 10 s
     * with the generated config file, so two heartbeats in a row may get lost before the gate closes.
     */
    static final Duration SILENT_AFTER = Duration.ofSeconds(25);

    private final GsiService gsiService;
    private final BooleanSupplier cs2Foreground;

    public FireGate(GsiService gsiService, BooleanSupplier cs2Foreground) {
        this.gsiService = gsiService;
        this.cs2Foreground = cs2Foreground;
    }

    public boolean isOpen() {
        return closedBecause().isEmpty();
    }

    /** Why no action may fire right now, as the text for the status line, or empty when one may. */
    public Optional<String> closedBecause() {
        return closedBecause(gsiService.isRunning(), gsiService.currentGameState(),
                gsiService.silence().orElse(Duration.ZERO))
                .or(() -> cs2Foreground.getAsBoolean() ? Optional.empty() : Optional.of("Waiting: CS2 is not in the foreground."));
    }

    /** {@code silence} is the time since {@code state} or a repeat of it came in. The empty state has none, it is not read then. */
    static Optional<String> closedBecause(boolean gsiRunning, GameState state, Duration silence) {
        if (!gsiRunning) {
            return Optional.of("Waiting for GSI. It was stopped, start it again in the \"Live (GSI)\" tab.");
        }
        if (state.player.activity == PlayerActivity.Undefined) { // the empty state before CS2 has sent anything
            return Optional.of("Waiting for data from CS2.");
        }
        if (silence.compareTo(SILENT_AFTER) > 0) {
            return Optional.of("Waiting: CS2 has sent nothing for over " + SILENT_AFTER.toSeconds() + " s.");
        }
        if (state.player.activity != PlayerActivity.Playing) {
            return Optional.of("Waiting: you are in the menu, the chat or the console.");
        }
        if (!state.isLocalPlayer()) { // CS2 fills the player node with whoever is on screen
            return Optional.of("Waiting: you are watching another player.");
        }
        if (state.player.state.health <= 0) {
            return Optional.of("Waiting: you are dead.");
        }
        if (FROZEN.contains(state.round.phase) || FROZEN.contains(state.phaseCountdowns.phase)) {
            return Optional.of("Waiting: freezetime, pause or timeout.");
        }
        return Optional.empty();
    }
}
