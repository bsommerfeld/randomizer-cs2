package de.bsommerfeld.randomizer.ui.overview;

import com.cs2gsi.nodes.Player;
import com.cs2gsi.nodes.PlayerState;
import com.cs2gsi.nodes.Weapon;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Builds the detail card for one player (title, HP/armor bars, money, round and match stats,
 * weapons) into a target pane. Only values CS2 actually sent are shown - CS2 (Source 2) often
 * omits fields and the library reports -1.
 */
final class PlayerDetailView {

    private PlayerDetailView() {
    }

    static void render(Player player, Pane target) {
        target.getChildren().clear();
        if (player == null || !player.isValid()) {
            target.getChildren().add(new Label("Keine Daten."));
            return;
        }
        String subtitle = GameTexts.team(player.team) + "  ·  " + GameTexts.activity(player.activity);
        target.getChildren().add(title(player.name.isBlank() ? "Spieler" : player.name, subtitle));

        target.getChildren().add(bar("HP", player.state.health));
        target.getChildren().add(bar("Armor", player.state.armor));
        PlayerState state = player.state;
        target.getChildren().add(kv("Helm", GameTexts.yesNo(state.hasHelmet)));
        target.getChildren().add(kv("Defuse-Kit", GameTexts.yesNo(state.hasDefuseKit)));
        addMoney(target, "Geld", state.money);
        if (state.flashAmount > 0) {
            target.getChildren().add(kv("Geblendet", state.flashAmount + "/255"));
        }
        if (state.burningAmount > 0) {
            target.getChildren().add(kv("Brennt", state.burningAmount + "/255"));
        }

        // Round stats only if CS2 actually sent them - showing "–" everywhere just looks broken.
        if (state.roundKills >= 0 || state.roundHSKills >= 0
                || state.roundTotalDamage >= 0 || state.equipmentValue >= 0) {
            target.getChildren().add(section("Diese Runde"));
            addNum(target, "Kills", state.roundKills);
            addNum(target, "davon Headshots", state.roundHSKills);
            addNum(target, "Schaden", state.roundTotalDamage);
            addMoney(target, "Equipment-Wert", state.equipmentValue);
        }

        target.getChildren().add(section("Match"));
        target.getChildren().add(kv("K / A / D", GameTexts.num(player.matchStats.kills) + " / "
                + GameTexts.num(player.matchStats.assists) + " / " + GameTexts.num(player.matchStats.deaths)));
        addNum(target, "MVPs", player.matchStats.mvps);
        addNum(target, "Score", player.matchStats.score);

        target.getChildren().add(section("Waffen"));
        if (player.weapons.isEmpty()) {
            target.getChildren().add(new Label("-"));
        } else {
            for (Weapon weapon : player.weapons) {
                target.getChildren().add(new Label(GameTexts.weaponLine(weapon)));
            }
        }
    }

    private static VBox title(String name, String subtitle) {
        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("detail-name");
        Label subLabel = new Label(subtitle);
        subLabel.getStyleClass().add("detail-subtitle");
        VBox box = new VBox(nameLabel, subLabel);
        box.getStyleClass().add("detail-title");
        return box;
    }

    private static Label section(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("detail-section");
        return label;
    }

    /** Adds a numeric row only when CS2 actually supplied the value (library returns -1 if absent). */
    private static void addNum(Pane target, String key, int value) {
        if (value >= 0) {
            target.getChildren().add(kv(key, String.valueOf(value)));
        }
    }

    /** Adds a money row only when the value is present. */
    private static void addMoney(Pane target, String key, int value) {
        if (value >= 0) {
            target.getChildren().add(kv(key, GameTexts.money(value)));
        }
    }

    private static HBox kv(String key, String value) {
        Label k = new Label(key);
        k.getStyleClass().add("detail-key");
        k.setMinWidth(130);
        Label v = new Label(value);
        v.getStyleClass().add("detail-value");
        HBox row = new HBox(k, v);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private static HBox bar(String key, int value) {
        Label k = new Label(key);
        k.getStyleClass().add("detail-key");
        k.setMinWidth(130);
        HBox row = new HBox(k);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setSpacing(6);
        if (value < 0) {
            row.getChildren().add(new Label("-"));
            return row;
        }
        ProgressBar progress = new ProgressBar(Math.min(1.0, value / 100.0));
        progress.getStyleClass().add(key.equals("Armor") ? "armor-bar" : "hp-bar");
        progress.setPrefWidth(140);
        Label amount = new Label(String.valueOf(value));
        amount.getStyleClass().add("detail-value");
        HBox.setHgrow(progress, Priority.NEVER);
        row.getChildren().addAll(progress, amount);
        return row;
    }
}
