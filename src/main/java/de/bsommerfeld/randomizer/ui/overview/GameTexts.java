package de.bsommerfeld.randomizer.ui.overview;

import com.cs2gsi.nodes.Bomb;
import com.cs2gsi.nodes.BombState;
import com.cs2gsi.nodes.PlayerActivity;
import com.cs2gsi.nodes.PlayerTeam;
import com.cs2gsi.nodes.Weapon;
import com.cs2gsi.nodes.WeaponState;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Game-state values as display text. The GSI library reports -1 for a number CS2 did not send. */
final class GameTexts {

    /** Stands in for every value CS2 did not send. */
    static final String ABSENT = "-";

    private GameTexts() {
    }

    static String num(int value) {
        return value < 0 ? ABSENT : String.valueOf(value);
    }

    static String money(int value) {
        return value < 0 ? ABSENT : "$" + value;
    }

    static String score(int value) {
        return value < 0 ? "0" : String.valueOf(value);
    }

    static String yesNo(boolean value) {
        return value ? "yes" : "no";
    }

    /** "m:ss", rounded up so the display never skips ahead of the game clock. */
    static String clock(double seconds) {
        int total = (int) Math.ceil(seconds);
        return total / 60 + ":" + String.format(Locale.US, "%02d", total % 60);
    }

    /** The timer label: "1:42", or "Bomb  0:31" once the bomb countdown has taken over. */
    static String countdown(RoundTimer.Remaining remaining) {
        return (remaining.bomb() ? "Bomb  " : "") + clock(remaining.seconds());
    }

    /**
     * The name the game shows ("AK-47", "USP-S"), which the GSI library knows for every weapon it
     * lists. One it does not list yet shows its payload name.
     */
    static String weaponName(Weapon weapon) {
        if (weapon == null || weapon.name.isBlank()) {
            return ABSENT;
        }
        return weapon.info.displayName.isBlank() ? weapon.name : weapon.info.displayName;
    }

    /**
     * One line per weapon: slot, name, ammo and state. The slot is the key that draws the weapon, which
     * the GSI library knows by name, so a weapon it does not list has none. The rest is only what CS2 sent.
     */
    static String weaponLine(Weapon weapon) {
        List<String> parts = new ArrayList<>();
        if (weapon.info.slot > 0) {
            parts.add("Slot " + weapon.info.slot);
        }
        parts.add(weaponName(weapon));
        if (weapon.ammoClip >= 0) {
            parts.add(ammo(weapon));
        }
        if (weapon.state != WeaponState.Undefined) {
            parts.add(weaponState(weapon.state));
        }
        return String.join("  ·  ", parts);
    }

    private static String weaponState(WeaponState state) {
        return switch (state) {
            case Active -> "in hand";
            case Holstered -> "holstered";
            case Reloading -> "reloading";
            case Undefined -> ABSENT;
        };
    }

    /** "17 / 40", or just the clip when CS2 sent no reserve, as for grenades. */
    private static String ammo(Weapon weapon) {
        return weapon.ammoReserve >= 0 ? weapon.ammoClip + " / " + weapon.ammoReserve : String.valueOf(weapon.ammoClip);
    }

    static String team(PlayerTeam team) {
        return switch (team) {
            case CT -> "Counter-Terrorists";
            case T -> "Terrorists";
            case Spectator -> "Spectator";
            case Undefined -> ABSENT;
        };
    }

    static String activity(PlayerActivity activity) {
        return switch (activity) {
            case Playing -> "playing";
            case Menu -> "in menu";
            case TextInput -> "typing";
            case Undefined -> ABSENT;
        };
    }

    /** The bomb's status with its countdown if CS2 sent one, empty when there is nothing to show. */
    static String bomb(BombState bombState, Bomb bomb) {
        String status = bombStatus(bombState);
        boolean hasCountdown = bomb.isValid() && bomb.countdown >= 0;
        return status.isEmpty() || !hasCountdown
                ? status
                : status + String.format(Locale.US, "  (%.1fs)", bomb.countdown);
    }

    private static String bombStatus(BombState bombState) {
        return switch (bombState) {
            case Carried -> "Bomb: carried";
            case Dropped -> "Bomb: dropped";
            case Planting -> "Bomb: being planted";
            case Planted -> "Bomb: planted";
            case Defusing -> "Bomb: being defused";
            case Defused -> "Bomb: defused";
            case Exploded -> "Bomb: exploded";
            case Undefined -> "";
        };
    }
}
