package de.bsommerfeld.randomizer.action;

import com.cs2gsi.nodes.Player;
import com.cs2gsi.nodes.Weapon;
import com.cs2gsi.nodes.WeaponInfo;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * One thing the randomizer can do. {@code name} is what the UI and the log show, {@code command} the
 * CS2 command the action stands for, whose key the UI lists.
 *
 * <p>{@code routes} says how the action can be played with the player as they are at that moment. A
 * route is the key presses in order, and the randomizer draws one of the routes. Most actions have
 * one route of one press. No route means the action is pointless right now, and the randomizer
 * picks among the others.
 *
 * <p>Two actions are equal only when they are the same catalog entry, a function has no value equality.
 */
public record Action(String name, String command, Function<Player, List<List<Step>>> routes) {

    /**
     * The command of the mouse move, which presses no key. CS2 has no such command. The tab and the
     * settings file only need a name for the action.
     */
    public static final String MOUSE_MOVE = "mouse_move";

    /** An action that holds its key the same way whatever the weapon. */
    public Action(String name, String command, int minHoldMillis, int maxHoldMillis) {
        this(name, command, Press.held(minHoldMillis, maxHoldMillis));
    }

    /** An action of one key that goes down the same way whatever the player carries. */
    Action(String name, String command, Press press) {
        this(name, command, anyPlayer -> List.of(List.of(new Step.OnKey(command, press))));
    }

    /**
     * An action of one key press that depends on the weapon in hand, because the same command needs a
     * hold with a rifle and repeated clicks with a pistol.
     */
    static Action byWeapon(String name, String command, Function<Weapon, Press> pressWith) {
        return byWeapon(name, command, anyWeapon -> true, pressWith);
    }

    /** The same, but pointless while the weapon in hand ignores the key. */
    static Action byWeapon(String name, String command, Predicate<Weapon> reactsTo, Function<Weapon, Press> pressWith) {
        return new Action(name, command, player -> {
            Weapon inHand = player.getActiveWeapon();
            return reactsTo.test(inHand) ? List.of(List.of(new Step.OnKey(command, pressWith.apply(inHand)))) : List.of();
        });
    }

    static Action tap(String name, String command) {
        return new Action(name, command, Press.tap());
    }

    static Action mouseMove(String name, Step.Turn turn) {
        return new Action(name, MOUSE_MOVE, anyPlayer -> List.of(List.of(turn)));
    }

    /**
     * The key of a weapon group, {@code slot4} for the grenades. Pointless while the player carries
     * nothing in that group ({@link WeaponInfo#slot}), a weapon the library does not list counts as none.
     */
    static Action slot(int slot) {
        String command = "slot" + slot;
        List<List<Step>> press = List.of(List.of(new Step.OnKey(command, Press.tap())));
        return new Action("Slot " + slot, command,
                player -> player.weapons.stream().anyMatch(weapon -> weapon.info.slot == slot) ? press : List.of());
    }
}
