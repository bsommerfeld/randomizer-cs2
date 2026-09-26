package de.bsommerfeld.randomizer.action;

import org.junit.jupiter.api.Test;

import java.util.List;

import static de.bsommerfeld.randomizer.action.ActionRunnerTest.playerCarrying;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ReloadingTest {

    private static final String KNIFE = "{\"name\":\"weapon_knife\",\"type\":\"Knife\",\"state\":\"%s\"}";
    private static final Step RELOAD_HELD = new Step.OnKey("+reload", Press.held(1500, 2000));

    @Test
    void aMagazineThatIsNotFullIsReloadedWithOneTap() {
        List<List<Step>> routes = Reloading.routes(playerCarrying(
                gun("weapon_ak47", "Rifle", 12, 30, 90, "active"),
                gun("weapon_glock", "Pistol", 3, 20, 120, "holstered")));

        assertEquals(List.of(List.of(new Step.OnKey("+reload", Press.tap()))), routes, "no switch to the pistol either");
    }

    @Test
    void aFullMagazineIsShotAtFirstOrLeftForAWeaponThatCanReload() {
        List<List<Step>> routes = Reloading.routes(playerCarrying(
                gun("weapon_ak47", "Rifle", 30, 30, 90, "active"),
                gun("weapon_glock", "Pistol", 3, 20, 120, "holstered"),
                KNIFE.formatted("holstered")));

        assertEquals(List.of(
                List.of(new Step.OnKey("+attack", Press.tap()), RELOAD_HELD),
                List.of(new Step.OnKey("slot2", Press.tap()), RELOAD_HELD)), routes);
    }

    @Test
    void theRevolverGetsTheHoldItsHammerNeedsForTheOneShot() {
        List<List<Step>> routes = Reloading.routes(playerCarrying(gun("weapon_revolver", "Pistol", 8, 8, 8, "active")));

        assertEquals(List.of(List.of(new Step.OnKey("+attack", Press.held(500, 700)), RELOAD_HELD)), routes);
    }

    @Test
    void aWeaponWithoutAMagazineIsNeverShotOnlySwitchedAwayFrom() {
        List<List<Step>> routes = Reloading.routes(playerCarrying(
                KNIFE.formatted("active"),
                gun("weapon_awp", "SniperRifle", 4, 5, 30, "holstered"),
                gun("weapon_glock", "Pistol", 20, 20, 120, "holstered")));

        assertEquals(List.of(List.of(new Step.OnKey("slot1", Press.tap()), RELOAD_HELD)), routes, "the glock is full");
    }

    @Test
    void thereIsNoRouteWhenNothingCanBeReloaded() {
        assertEquals(List.of(), Reloading.routes(playerCarrying(
                KNIFE.formatted("active"),
                gun("weapon_glock", "Pistol", 20, 20, 120, "holstered"))), "knife in hand, full pistol");
        assertEquals(List.of(), Reloading.routes(playerCarrying(
                gun("weapon_ak47", "Rifle", 30, 30, 0, "active"),
                gun("weapon_glock", "Pistol", 3, 20, 0, "holstered"))), "no reserve ammo anywhere");
        assertEquals(List.of(), Reloading.routes(playerCarrying()), "no weapon known");
    }

    private static String gun(String name, String type, int clip, int clipMax, int reserve, String state) {
        return ("{\"name\":\"%s\",\"type\":\"%s\",\"ammo_clip\":%d,\"ammo_clip_max\":%d,\"ammo_reserve\":%d,"
                + "\"state\":\"%s\"}").formatted(name, type, clip, clipMax, reserve, state);
    }
}
