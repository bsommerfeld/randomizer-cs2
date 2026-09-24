package de.bsommerfeld.randomizer.action;

import com.cs2gsi.nodes.Player;
import org.junit.jupiter.api.Test;

import java.util.List;

import static de.bsommerfeld.randomizer.action.ActionRunnerTest.playerCarrying;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ActionTest {

    @Test
    void aSlotKeyIsPressedOnlyWhileSomethingIsCarriedInThatSlot() {
        Player pistolAndKnife = playerCarrying(
                "{\"name\":\"weapon_knife\",\"state\":\"active\"}",
                "{\"name\":\"weapon_glock\",\"state\":\"holstered\"}");

        assertEquals(List.of(List.of(new Step("slot2", Press.tap()))), Action.slot(2).routes().apply(pistolAndKnife));
        assertEquals(List.of(List.of(new Step("slot3", Press.tap()))), Action.slot(3).routes().apply(pistolAndKnife));
        assertEquals(List.of(), Action.slot(1).routes().apply(pistolAndKnife), "no primary");
        assertEquals(List.of(), Action.slot(4).routes().apply(pistolAndKnife), "no grenade");
        assertEquals(List.of(), Action.slot(2).routes().apply(playerCarrying()), "no weapon known");
    }

    @Test
    void secondaryFireOnlyWithAWeaponThatReactsToIt() {
        Action secondary = ActionCatalog.ALL.stream()
                .filter(action -> action.command().equals("+attack2"))
                .findFirst().orElseThrow();

        assertEquals(List.of(List.of(new Step("+attack2", Press.held(100, 800)))),
                secondary.routes().apply(playerCarrying("{\"name\":\"weapon_awp\",\"state\":\"active\"}")));
        assertEquals(List.of(), secondary.routes().apply(playerCarrying("{\"name\":\"weapon_ak47\",\"state\":\"active\"}")));
        assertEquals(List.of(), secondary.routes().apply(playerCarrying()), "no weapon known");
    }

    @Test
    void aGrenadeFillsSlot4() {
        Player knifeAndFlash = playerCarrying(
                "{\"name\":\"weapon_knife\",\"state\":\"active\"}",
                "{\"name\":\"weapon_flashbang\",\"state\":\"holstered\"}");

        assertEquals(List.of(List.of(new Step("slot4", Press.tap()))), Action.slot(4).routes().apply(knifeAndFlash));
    }
}
