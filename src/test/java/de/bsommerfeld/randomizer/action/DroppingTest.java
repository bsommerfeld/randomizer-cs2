package de.bsommerfeld.randomizer.action;

import org.junit.jupiter.api.Test;

import java.util.List;

import static de.bsommerfeld.randomizer.action.ActionRunnerTest.playerCarrying;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DroppingTest {

    private static final Step DROP = new Step("drop", Press.tap());
    private static final Step SLOT1 = new Step("slot1", Press.held(100, 200));
    private static final Step SLOT2 = new Step("slot2", Press.held(100, 200));
    private static final Step SLOT5 = new Step("slot5", Press.held(100, 200));
    private static final Step FLASH = new Step(List.of("slot7", "slot4"), Press.held(100, 200));
    private static final Step SMOKE = new Step(List.of("slot8", "slot4"), Press.held(100, 200));
    private static final Step ZEUS = new Step("slot11", Press.held(100, 200));

    @Test
    void theWeaponInHandAloneOrAnySelectionOfTheCarriedOnesInSlotOrder() {
        List<List<Step>> routes = Dropping.routes(playerCarrying(
                carried("weapon_knife", "Knife", "holstered"),
                carried("weapon_glock", "Pistol", "holstered"), // GSI lists the pistol before the rifle
                carried("weapon_ak47", "Rifle", "active")));

        assertEquals(List.of(
                List.of(DROP),
                List.of(SLOT1, DROP),
                List.of(SLOT2, DROP),
                List.of(SLOT1, DROP, SLOT2, DROP)), routes);
    }

    @Test
    void theKnifeIsNeverDroppedButWhatIsCarriedBesideItCanBe() {
        assertEquals(List.of(), Dropping.routes(playerCarrying(carried("weapon_knife", "Knife", "active"))));
        assertEquals(List.of(), Dropping.routes(playerCarrying()), "no weapon known");
        assertEquals(List.of(List.of(SLOT2, DROP)), Dropping.routes(playerCarrying(
                carried("weapon_knife_karambit", "Knife", "active"),
                carried("weapon_glock", "Pistol", "holstered"))));
    }

    @Test
    void grenadesAndTheBombAreInTheSelectionsEachGrenadeByItsOwnKeyOrElseTheGroups() {
        List<List<Step>> routes = Dropping.routes(playerCarrying(
                carried("weapon_knife", "Knife", "active"),
                carried("weapon_smokegrenade", "Grenade", "holstered"), // GSI lists the smoke before the flash
                carried("weapon_flashbang", "Grenade", "holstered"),
                carried("weapon_c4", "C4", "holstered")));

        assertEquals(7, routes.size(), "every selection of three, none for the knife in hand");
        assertTrue(routes.contains(List.of(FLASH, DROP, SMOKE, DROP, SLOT5, DROP)), "everything goes: " + routes);
    }

    @Test
    void theZeusIsDrawnByItsOwnKeyOnlySlot3CanLandOnTheKnife() {
        assertEquals(List.of(List.of(DROP), List.of(ZEUS, DROP)), Dropping.routes(playerCarrying(
                "{\"name\":\"weapon_taser\",\"state\":\"active\"}",
                carried("weapon_knife", "Knife", "holstered"))));
    }

    @Test
    void aWeaponTheLibraryDoesNotListGoesFromTheHandOnly() {
        assertEquals(List.of(List.of(DROP)), Dropping.routes(playerCarrying(
                carried("weapon_new_gun", "Rifle", "active"))));
        assertEquals(List.of(), Dropping.routes(playerCarrying(
                carried("weapon_knife", "Knife", "active"),
                carried("weapon_new_gun", "Rifle", "holstered"))));
    }

    private static String carried(String name, String type, String state) {
        return "{\"name\":\"%s\",\"type\":\"%s\",\"state\":\"%s\"}".formatted(name, type, state);
    }
}
