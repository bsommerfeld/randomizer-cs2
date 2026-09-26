package de.bsommerfeld.randomizer.action;

import java.util.List;

/** Every action the randomizer knows. */
public final class ActionCatalog {

    /**
     * CS2 jumps once per key down, a held key is still one jump. So the key is clicked, one to four
     * times, with a pause as long as a jump: 0.76 s from the ground back to the ground. A click that
     * comes while still in the air is lost.
     */
    private static final Press JUMPS = Press.paced(500, 3000, 750, 900);

    /**
     * Up to 3000 counts sideways, 66 degrees at sensitivity 1 and 165 at 2.5. Up or down only a fifth
     * of that, a view stuck on the floor is no fun for long.
     *
     * <p>ponytail: fixed counts, so the turn grows with the player's sensitivity. Read {@code sensitivity}
     * from the config and aim for degrees if the spread across players is too wide.
     */
    private static final Step.Turn TURN = new Step.Turn(3000, 1000, 150, 600);

    public static final List<Action> ALL = List.of(
            Action.byWeapon("Shoot", "+attack", FireModes::primary),
            Action.byWeapon("Secondary fire", "+attack2", weapon -> weapon.info.hasSecondaryFire, FireModes::secondary),
            new Action("Reload", Reloading.COMMAND, Reloading::routes),
            new Action("Drop weapon", Dropping.COMMAND, Dropping::routes),
            new Action("Jump", "+jump", JUMPS),
            new Action("Crouch", "+duck", 300, 2000),
            new Action("Forward", "+forward", 500, 3000),
            new Action("Back", "+back", 500, 3000),
            new Action("Left", "+left", 500, 3000),
            new Action("Right", "+right", 500, 3000),
            new Action("Walk", "+sprint", 500, 3000),
            Action.slot(1),
            Action.slot(2),
            Action.slot(3),
            Action.slot(4),
            Action.tap("Last weapon", "lastinv"),
            Action.mouseMove("Mouse move", TURN));

    private ActionCatalog() {
    }
}
