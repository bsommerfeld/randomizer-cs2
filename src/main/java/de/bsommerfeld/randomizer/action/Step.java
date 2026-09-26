package de.bsommerfeld.randomizer.action;

import java.util.List;

/** One part of an action's route: a key press or a turn of the view. */
public sealed interface Step {

    /**
     * One key press, going down the way {@code press} says. Every one of {@code commands} does the
     * step's job, the best one first. The randomizer presses the key of the first one that has a key.
     */
    record OnKey(List<String> commands, Press press) implements Step {

        /** A step that only one command can do. */
        public OnKey(String command, Press press) {
            this(List.of(command), press);
        }
    }

    /**
     * A mouse move by a random amount, up to {@code maxX} counts sideways and {@code maxY} up or down,
     * spread over a random time within the range. How far a count turns the view depends on the
     * player's sensitivity: {@code sensitivity * 0.022} degrees.
     */
    record Turn(int maxX, int maxY, int minMillis, int maxMillis) implements Step {
    }
}
