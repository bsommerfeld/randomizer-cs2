package de.bsommerfeld.randomizer.ui.overview;

import com.cs2gsi.nodes.Bomb;
import com.cs2gsi.nodes.BombState;
import com.cs2gsi.nodes.Weapon;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GameTextsTest {

    @Test
    void weaponNameIsTheGamesOwnAndThePayloadNameForUnlistedWeapons() {
        assertEquals("USP-S", GameTexts.weaponName(weapon("{\"name\":\"weapon_usp_silencer\"}", 1)));
        assertEquals("AK-47", GameTexts.weaponName(weapon("{\"name\":\"weapon_ak47\"}", 2)));
        assertEquals("weapon_new_gun", GameTexts.weaponName(weapon("{\"name\":\"weapon_new_gun\"}", 2)));
        assertEquals("-", GameTexts.weaponName(null));
    }

    @Test
    void weaponLineListsOnlyWhatCs2Sent() {
        assertEquals("Slot 1  ·  M4A1-S  ·  17 / 40  ·  in hand", GameTexts.weaponLine(weapon(
                "{\"name\":\"weapon_m4a1_silencer\",\"ammo_clip\":17,\"ammo_reserve\":40,\"state\":\"active\"}", 2)));
        assertEquals("Slot 3  ·  Knife  ·  holstered", GameTexts.weaponLine(weapon(
                "{\"name\":\"weapon_knife\",\"state\":\"holstered\"}", 0)));
        assertEquals("weapon_new_gun  ·  holstered", GameTexts.weaponLine(weapon(
                "{\"name\":\"weapon_new_gun\",\"state\":\"holstered\"}", 1)));
    }

    @Test
    void clockRoundsUpSoItNeverRunsAheadOfTheGame() {
        assertEquals("1:55", GameTexts.clock(114.2));
        assertEquals("0:40", GameTexts.clock(39.9));
        assertEquals("0:00", GameTexts.clock(0));
    }

    @Test
    void countdownNamesTheBombOnceItsTimerTookOver() {
        assertEquals("1:55", GameTexts.countdown(new RoundTimer.Remaining(115, false)));
        assertEquals("Bomb  0:40", GameTexts.countdown(new RoundTimer.Remaining(40, true)));
    }

    @Test
    void bombLineIsEmptyWithoutABombStateAndPlainWithoutACountdown() {
        assertEquals("", GameTexts.bomb(BombState.Undefined, new Bomb()));
        assertEquals("Bomb: planted", GameTexts.bomb(BombState.Planted, new Bomb()));
    }

    private static Weapon weapon(String json, int index) {
        return new Weapon(JsonParser.parseString(json).getAsJsonObject(), index);
    }
}
