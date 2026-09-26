package de.bsommerfeld.randomizer.action;

import com.cs2gsi.nodes.Player;
import com.cs2gsi.nodes.Weapon;

import java.util.ArrayList;
import java.util.List;

/**
 * When the reload key is worth pressing, and what has to come before it. CS2 ignores the key with a
 * full magazine and with a weapon that has none. The action then makes room first: one shot with the
 * weapon in hand, or a switch to a weapon whose magazine is not full.
 */
final class Reloading {

    static final String COMMAND = "+reload";

    /**
     * After a shot or a switch the weapon takes no reload until its cycle or draw time is over, 1.46 s
     * with the AWP. The key is held across that, CS2 starts the reload the moment the weapon allows it.
     */
    private static final Press HELD_UNTIL_READY = Press.held(1500, 2000);

    private Reloading() {
    }

    static List<List<Step>> routes(Player player) {
        Weapon inHand = player.getActiveWeapon();
        if (canReload(inHand)) {
            return List.of(List.of(new Step.OnKey(COMMAND, Press.tap())));
        }
        List<List<Step>> routes = new ArrayList<>();
        if (canReloadAfterAShot(inHand)) {
            routes.add(List.of(new Step.OnKey("+attack", FireModes.singleShot(inHand)), new Step.OnKey(COMMAND, HELD_UNTIL_READY)));
        }
        for (Weapon carried : player.weapons) {
            List<String> slot = Slots.commandsFor(carried.info);
            if (canReload(carried) && !slot.isEmpty()) {
                routes.add(List.of(new Step.OnKey(slot, Press.tap()), new Step.OnKey(COMMAND, HELD_UNTIL_READY)));
            }
        }
        return routes;
    }

    /** Knife, grenades and C4 report no magazine at all. Without reserve ammo the key does nothing either. */
    private static boolean canReload(Weapon weapon) {
        return weapon.ammoClipMax > 0 && weapon.ammoClip < weapon.ammoClipMax && weapon.ammoReserve > 0;
    }

    private static boolean canReloadAfterAShot(Weapon weapon) {
        return weapon.ammoClipMax > 0 && weapon.ammoClip == weapon.ammoClipMax && weapon.ammoReserve > 0;
    }
}
