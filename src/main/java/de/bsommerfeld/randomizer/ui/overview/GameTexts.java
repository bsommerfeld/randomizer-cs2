package de.bsommerfeld.randomizer.ui.overview;

import com.cs2gsi.GameState;
import com.cs2gsi.nodes.GameMode;
import com.cs2gsi.nodes.PlayerActivity;
import com.cs2gsi.nodes.PlayerTeam;
import com.cs2gsi.nodes.Weapon;
import com.cs2gsi.nodes.WeaponState;

import java.util.Locale;

/**
 * Renders game-state values as display text (German UI wording). Sentinel-aware: the GSI library
 * returns -1 for absent numeric fields.
 */
final class GameTexts {

    private GameTexts() {
    }

    /** Sentinel-aware integer: the library returns -1 for absent numeric fields. */
    static String num(int value) {
        return value < 0 ? "–" : String.valueOf(value);
    }

    static String money(int value) {
        return value < 0 ? "–" : "$" + value;
    }

    static String score(int value) {
        return value < 0 ? "0" : String.valueOf(value);
    }

    static String yesNo(boolean value) {
        return value ? "ja" : "nein";
    }

    /** "m:ss", rounded up so the display never skips ahead of the game clock. */
    static String clock(double seconds) {
        int total = (int) Math.ceil(seconds);
        return total / 60 + ":" + String.format(Locale.US, "%02d", total % 60);
    }

    /** "weapon_ak47" → "Ak47", "weapon_usp_silencer" → "Usp Silencer". */
    static String weaponName(Weapon weapon) {
        if (weapon == null || weapon.name.isBlank()) {
            return "-";
        }
        String name = weapon.name.startsWith("weapon_") ? weapon.name.substring("weapon_".length()) : weapon.name;
        String[] words = name.split("_");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return sb.toString();
    }

    /** One line per weapon: slot, name, ammo and state - only what CS2 actually sent. */
    static String weaponLine(Weapon weapon) {
        StringBuilder sb = new StringBuilder();
        if (weapon.slot >= 0) {
            sb.append("Slot ").append(weapon.slot).append("  ·  ");
        }
        sb.append(weaponName(weapon));
        if (weapon.ammoClip >= 0) {
            sb.append("  ·  ").append(weapon.ammoClip);
            if (weapon.ammoReserve >= 0) {
                sb.append(" / ").append(weapon.ammoReserve);
            }
        }
        if (weapon.state != WeaponState.Undefined) {
            sb.append("  ·  ").append(weapon.state);
        }
        return sb.toString();
    }

    static String team(PlayerTeam team) {
        return switch (team) {
            case CT -> "Counter-Terrorists";
            case T -> "Terrorists";
            case Spectator -> "Zuschauer";
            case Undefined -> "-";
        };
    }

    static String activity(PlayerActivity activity) {
        return switch (activity) {
            case Playing -> "spielt";
            case Menu -> "im Menü";
            case TextInput -> "tippt";
            case Undefined -> "-";
        };
    }

    /** Bomb status line incl. countdown if CS2 sent one; empty when there is nothing to show. */
    static String bomb(GameState state) {
        String base = switch (state.round.bombState) {
            case Carried -> "Bombe: getragen";
            case Dropped -> "Bombe: fallen gelassen";
            case Planting -> "Bombe: wird gelegt";
            case Planted -> "Bombe: gelegt";
            case Defusing -> "Bombe: wird entschärft";
            case Defused -> "Bombe: entschärft";
            case Exploded -> "Bombe: explodiert";
            case Undefined -> "";
        };
        if (base.isEmpty()) {
            return "";
        }
        if (state.bomb.isValid() && state.bomb.countdown >= 0) {
            base += String.format(Locale.US, "  (%.1fs)", state.bomb.countdown);
        }
        return base;
    }

    static String mode(GameMode mode) {
        return switch (mode) {
            case Competitive -> "Competitive";
            case Scrimcomp2v2 -> "Wingman";
            case Scrimcomp5v5 -> "Weapons Expert";
            case Casual -> "Casual";
            case Deathmatch -> "Deathmatch";
            case Custom -> "Custom";
            case Skirmish -> "Skirmish";
            case Cooperative -> "Co-op";
            case Training -> "Training";
            case Undefined -> "";
        };
    }
}
