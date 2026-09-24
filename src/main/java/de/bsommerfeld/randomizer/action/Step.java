package de.bsommerfeld.randomizer.action;

import java.util.List;

/**
 * One key press within an action, going down the way {@code press} says. Every one of {@code commands}
 * does the step's job, the best one first. The randomizer presses the key of the first one that has a key.
 */
public record Step(List<String> commands, Press press) {

    /** A step that only one command can do. */
    public Step(String command, Press press) {
        this(List.of(command), press);
    }
}
