package de.bsommerfeld.randomizer.action;

import com.cs2gsi.nodes.Player;
import com.cs2gsi.nodes.Weapon;
import com.cs2gsi.nodes.WeaponInfo;
import com.cs2gsi.nodes.WeaponType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * What the drop action can throw away. CS2 drops the weapon in hand, so another weapon has to be
 * drawn first. The routes are the weapon in hand alone and every selection of the carried weapons,
 * from a single one up to all of them, walked through in slot order. The randomizer draws one
 * route, so the more the player carries, the rarer it stays at the weapon in hand. {@link Slots}
 * says which keys draw a weapon, one it has no key for goes from the hand only.
 */
final class Dropping {

    static final String COMMAND = "drop";

    /**
     * Longer than a tap. A drop that reaches the server before the switch throws the old weapon
     * away. The bomb drop binds players use get the same gap from the key's way down and up.
     */
    private static final Press SWITCH = Press.held(100, 200);

    /** Primary, pistol, Zeus, the grenades the way their own keys lie, the bomb. */
    private static final Comparator<WeaponInfo> IN_SLOT_ORDER =
            Comparator.comparingInt((WeaponInfo weapon) -> weapon.slot).thenComparingInt(weapon -> weapon.directSlot);

    /** A match hands out seven at most. The selections double with every weapon, a mod with thirty would stall the draw. */
    private static final int MOST_WEAPONS = 10;

    private Dropping() {
    }

    static List<List<Step>> routes(Player player) {
        List<List<Step>> routes = new ArrayList<>();
        if (canBeDropped(player.getActiveWeapon())) {
            routes.add(List.of(new Step(COMMAND, Press.tap())));
        }
        List<Step> switches = player.weapons.stream()
                .filter(Dropping::canBeDropped)
                .map(weapon -> weapon.info)
                .sorted(IN_SLOT_ORDER)
                .map(Slots::commandsFor)
                .filter(commands -> !commands.isEmpty())
                .limit(MOST_WEAPONS)
                .map(commands -> new Step(commands, SWITCH))
                .toList();
        for (int selection = 1; selection < 1 << switches.size(); selection++) {
            routes.add(switchAndDropEach(switches, selection));
        }
        return routes;
    }

    /** CS2 keeps the knife in the player's hand whatever they press. An empty name is no weapon at all. */
    private static boolean canBeDropped(Weapon weapon) {
        return !weapon.name.isEmpty() && weapon.type != WeaponType.Knife;
    }

    /** {@code selection} has bit i set when the weapon that {@code switches.get(i)} draws goes. */
    private static List<Step> switchAndDropEach(List<Step> switches, int selection) {
        List<Step> route = new ArrayList<>();
        for (int i = 0; i < switches.size(); i++) {
            if ((selection & 1 << i) != 0) {
                route.add(switches.get(i));
                route.add(new Step(COMMAND, Press.tap()));
            }
        }
        return route;
    }
}
