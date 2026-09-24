package de.bsommerfeld.randomizer.action;

import com.cs2gsi.nodes.Weapon;
import org.junit.jupiter.api.Test;

import static de.bsommerfeld.randomizer.action.ActionRunnerTest.weapon;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FireModesTest {

    @Test
    void weaponsThatFireOncePerClickAreSpammed() {
        assertEquals(Press.spammed(200, 1500), FireModes.primary(weapon("weapon_glock", "Pistol")));
        assertEquals(Press.spammed(200, 1500), FireModes.primary(weapon("weapon_deagle", "Pistol")));
        assertEquals(Press.spammed(200, 1500), FireModes.primary(weapon("weapon_nova", "Shotgun")));
    }

    @Test
    void boltActionSnipersAreClickedOncePerBoltCycle() {
        Press awp = FireModes.primary(weapon("weapon_awp", "SniperRifle"));

        assertEquals(Press.paced(2000, 4500, 1500, 1800), awp);
        assertEquals(awp, FireModes.primary(weapon("weapon_ssg08", "SniperRifle")));
        assertTrue(awp.minMillis() > awp.maxPauseMillis() + 60, "every window fits a second shot");
    }

    @Test
    void weaponsThatKeepFiringAreHeld() {
        assertFalse(FireModes.primary(weapon("weapon_ak47", "Rifle")).clicks());
        assertFalse(FireModes.primary(weapon("weapon_cz75a", "Pistol")).clicks(), "the full-auto pistol");
        assertFalse(FireModes.primary(weapon("weapon_xm1014", "Shotgun")).clicks(), "the full-auto shotgun");
        assertFalse(FireModes.primary(weapon("weapon_scar20", "SniperRifle")).clicks(), "the CT auto sniper");
        assertFalse(FireModes.primary(weapon("weapon_g3sg1", "SniperRifle")).clicks(), "the T auto sniper");
        assertFalse(FireModes.primary(weapon("weapon_knife", "Knife")).clicks());
        assertFalse(FireModes.primary(new Weapon()).clicks(), "no weapon known");
    }

    @Test
    void theRevolverTurnsBothKeysAround() {
        Weapon revolver = weapon("weapon_revolver", "Pistol");

        assertEquals(Press.held(500, 1500), FireModes.primary(revolver), "long enough for the hammer");
        assertTrue(FireModes.secondary(revolver).clicks());
        assertFalse(FireModes.secondary(weapon("weapon_awp", "SniperRifle")).clicks(), "everything else holds its second key");
    }
}
