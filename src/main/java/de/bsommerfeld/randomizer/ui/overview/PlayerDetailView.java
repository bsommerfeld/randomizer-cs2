package de.bsommerfeld.randomizer.ui.overview;

import com.cs2gsi.nodes.MatchStats;
import com.cs2gsi.nodes.Player;
import com.cs2gsi.nodes.PlayerActivity;
import com.cs2gsi.nodes.PlayerState;
import com.cs2gsi.nodes.Weapon;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Builds the detail card for one player into a target pane. CS2 often leaves fields out, and the
 * library reports -1 for them. The card shows only what CS2 sent.
 */
final class PlayerDetailView {

    private static final double KEY_COLUMN_WIDTH = 130;

    private PlayerDetailView() {
    }

    static void render(Player player, Pane target) {
        boolean known = player != null && player.isValid();
        target.getChildren().setAll(known ? card(player) : List.of(new Label("No data.")));
    }

    private static List<Node> card(Player player) {
        List<Node> rows = new ArrayList<>();
        rows.add(title(player));
        rows.addAll(vitals(player.state));
        rows.addAll(roundStats(player.state));
        rows.addAll(matchStats(player.matchStats));
        rows.addAll(weapons(player.weapons));
        return rows;
    }

    private static VBox title(Player player) {
        Label name = new Label(player.name.isBlank() ? "Player" : player.name);
        name.getStyleClass().add("detail-name");
        Label subtitle = new Label(subtitle(player));
        subtitle.getStyleClass().add("detail-subtitle");
        VBox box = new VBox(name, subtitle);
        box.getStyleClass().add("detail-title");
        return box;
    }

    /** CS2 sends the activity only for the local player. For everyone else the team stands alone. */
    private static String subtitle(Player player) {
        return player.activity == PlayerActivity.Undefined
                ? GameTexts.team(player.team)
                : GameTexts.team(player.team) + ", " + GameTexts.activity(player.activity);
    }

    private static List<Node> vitals(PlayerState state) {
        List<Node> rows = new ArrayList<>();
        rows.add(bar("HP", state.health, "hp-bar"));
        rows.add(bar("Armor", state.armor, "armor-bar"));
        rows.add(kv("Helmet", GameTexts.yesNo(state.hasHelmet)));
        rows.add(kv("Defuse kit", GameTexts.yesNo(state.hasDefuseKit)));
        money("Money", state.money).ifPresent(rows::add);
        if (state.flashAmount > 0) {
            rows.add(kv("Flashed", state.flashAmount + "/255"));
        }
        if (state.burningAmount > 0) {
            rows.add(kv("Burning", state.burningAmount + "/255"));
        }
        return rows;
    }

    /** Empty when CS2 sent none of the round values. A section full of blanks looks broken. */
    private static List<Node> roundStats(PlayerState state) {
        List<Node> rows = new ArrayList<>();
        num("Kills", state.roundKills).ifPresent(rows::add);
        num("of them headshots", state.roundHSKills).ifPresent(rows::add);
        num("Damage", state.roundTotalDamage).ifPresent(rows::add);
        money("Equipment value", state.equipmentValue).ifPresent(rows::add);
        if (!rows.isEmpty()) {
            rows.addFirst(section("This round"));
        }
        return rows;
    }

    private static List<Node> matchStats(MatchStats stats) {
        List<Node> rows = new ArrayList<>();
        rows.add(section("Match"));
        rows.add(kv("K / A / D", GameTexts.num(stats.kills) + " / "
                + GameTexts.num(stats.assists) + " / " + GameTexts.num(stats.deaths)));
        num("MVPs", stats.mvps).ifPresent(rows::add);
        num("Score", stats.score).ifPresent(rows::add);
        return rows;
    }

    private static List<Node> weapons(List<Weapon> weapons) {
        List<Node> rows = new ArrayList<>();
        rows.add(section("Weapons"));
        if (weapons.isEmpty()) {
            rows.add(new Label(GameTexts.ABSENT));
        }
        weapons.forEach(weapon -> rows.add(weaponLine(weapon)));
        return rows;
    }

    private static Label weaponLine(Weapon weapon) {
        Label line = new Label(GameTexts.weaponLine(weapon));
        line.setWrapText(true);
        return line;
    }

    private static Label section(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("detail-section");
        return label;
    }

    /** A numeric row, or empty when CS2 did not send the value. */
    private static Optional<Node> num(String key, int value) {
        return value < 0 ? Optional.empty() : Optional.of(kv(key, String.valueOf(value)));
    }

    private static Optional<Node> money(String key, int value) {
        return value < 0 ? Optional.empty() : Optional.of(kv(key, GameTexts.money(value)));
    }

    private static HBox kv(String key, String value) {
        return row(keyLabel(key), valueLabel(value));
    }

    /** A 0 to 100 value as a bar and its number. {@code styleClass} picks the bar's color. */
    private static HBox bar(String key, int value, String styleClass) {
        if (value < 0) {
            return row(keyLabel(key), new Label(GameTexts.ABSENT));
        }
        ProgressBar progress = new ProgressBar(Math.min(1.0, value / 100.0));
        progress.getStyleClass().add(styleClass);
        progress.setPrefWidth(100);
        Label amount = valueLabel(String.valueOf(value));
        amount.setMinWidth(Region.USE_PREF_SIZE); // in a narrow card the bar shrinks, never the number
        return row(keyLabel(key), progress, amount);
    }

    private static HBox row(Node... cells) {
        HBox row = new HBox(6, cells);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private static Label keyLabel(String key) {
        Label label = new Label(key);
        label.getStyleClass().add("detail-key");
        label.setMinWidth(KEY_COLUMN_WIDTH);
        return label;
    }

    private static Label valueLabel(String value) {
        Label label = new Label(value);
        label.getStyleClass().add("detail-value");
        return label;
    }
}
