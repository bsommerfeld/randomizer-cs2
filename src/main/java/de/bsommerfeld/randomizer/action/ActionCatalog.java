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
            Action.tap("Last weapon", "lastinv"));

    private ActionCatalog() {
    }
}
