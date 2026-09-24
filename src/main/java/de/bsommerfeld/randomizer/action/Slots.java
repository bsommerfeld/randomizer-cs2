package de.bsommerfeld.randomizer.action;

import com.cs2gsi.nodes.WeaponInfo;

import java.util.List;
import java.util.stream.IntStream;

/** Which slot commands draw a carried weapon. The numbers are the GSI library's knowledge ({@link WeaponInfo#slot}). */
final class Slots {

    /** The knife sits there beside the Zeus, a press can land on either. So this slot draws nothing for certain. */
    private static final int KNIFE_SLOT = 3;

    private Slots() {
    }

    /**
     * First the command that draws exactly this weapon, {@code slot7} for the flashbang, then the one
     * of its group, {@code slot4}, where the grenade that comes out is the game's call. The second is
     * for a player who took the first off its key. Most weapons have the group command only, the Zeus
     * its own only. Empty for the knife and for a weapon the library does not list.
     */
    static List<String> commandsFor(WeaponInfo weapon) {
        return IntStream.of(weapon.directSlot, weapon.slot)
                .filter(slot -> slot > 0 && slot != KNIFE_SLOT)
                .mapToObj(slot -> "slot" + slot)
                .toList();
    }
}
