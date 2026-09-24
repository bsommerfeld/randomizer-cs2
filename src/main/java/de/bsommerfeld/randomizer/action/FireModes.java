package de.bsommerfeld.randomizer.action;

import com.cs2gsi.nodes.FireMode;
import com.cs2gsi.nodes.Weapon;
import com.cs2gsi.nodes.WeaponInfo;

/**
 * How the two attack keys have to go down so the weapon in hand fires for the whole action. CS2
 * shoots once per click with most pistols, shotguns and sniper rifles, a held key gives one shot
 * there. Which weapon fires how is the GSI library's knowledge ({@link WeaponInfo#fireMode}), this
 * class only turns it into a press.
 */
final class FireModes {

    /**
     * One click per bolt cycle. The AWP needs about 1.46 s between two shots, the SSG 08 1.25 s. The
     * window fits two or three shots. Spamming would fire as well, but with some twenty clicks per shot.
     */
    private static final Press BOLT_ACTION = Press.paced(2000, 4500, 1500, 1800);

    private FireModes() {
    }

    static Press primary(Weapon weapon) {
        return switch (weapon.info.fireMode) {
            case Automatic -> Press.held(200, 1500);
            case SemiAutomatic -> Press.spammed(200, 1500);
            case BoltAction -> BOLT_ACTION;
            case Revolver -> Press.held(500, 1500); // the hammer has to come back first, a short hold fires nothing
            case Undefined -> Press.held(200, 1500); // grenades, C4 and a weapon the library does not list yet
        };
    }

    /** One shot, or a few from a fast automatic. The revolver fires nothing on a tap, its hammer takes 0.4 s. */
    static Press singleShot(Weapon weapon) {
        return weapon.info.fireMode == FireMode.Revolver ? Press.held(500, 700) : Press.tap();
    }

    static Press secondary(Weapon weapon) {
        if (weapon.info == WeaponInfo.R8Revolver) {
            return Press.spammed(400, 1500); // fanning the hammer, one shot per click
        }
        return Press.held(100, 800);
    }
}
