package de.bsommerfeld.randomizer.ui;

import com.cs2gsi.GameState;
import com.cs2gsi.nodes.BombState;
import com.cs2gsi.nodes.GameMode;
import com.cs2gsi.nodes.Phase;
import com.cs2gsi.nodes.Player;
import com.cs2gsi.nodes.PlayerActivity;
import com.cs2gsi.nodes.PlayerState;
import com.cs2gsi.nodes.PlayerTeam;
import com.cs2gsi.nodes.Weapon;
import de.bsommerfeld.randomizer.gsi.GsiService;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * Drives the live "Übersicht" tab: a team-grouped scoreboard of all players plus a detail panel for
 * the selected one, fed by the structured {@link GameState} stream from {@link GsiService}.
 *
 * <p>CS2 only fills {@code allPlayers} in spectator/observer mode; in a normal match that block is
 * empty and this view falls back to a hint plus the local player's own card.
 *
 * <p>All GSI callbacks arrive on the listener thread and are marshalled onto the JavaFX thread via
 * {@link Platform#runLater} before touching any control.
 */
public final class OverviewController {

    @FXML private Label mapLabel;
    @FXML private Label timeLabel;
    @FXML private Label ctScoreLabel;
    @FXML private Label tScoreLabel;
    @FXML private Label bombLabel;

    @FXML private Pane scoreboardBox;
    @FXML private TableView<Player> ctTable;
    @FXML private TableView<Player> tTable;
    @FXML private VBox detailContent;

    @FXML private Pane placeholderBox;
    @FXML private Label placeholderLabel;
    @FXML private VBox ownPlayerContent;

    private final GsiService gsiService;

    /** Steam ID of the row whose details are shown, so selection survives item replacement. */
    private String selectedSteamId;
    /** Steam ID of the local player, used to highlight their scoreboard row. */
    private String localSteamId;

    /** Epoch millis when the active countdown reaches zero, or negative when no timer runs. */
    private long deadlineMillis = -1;
    /** Whether the running timer is the bomb (40s) rather than the round (1:55). */
    private boolean bombTimer;
    private Phase prevPhase = Phase.Undefined;
    private BombState prevBomb = BombState.Undefined;

    public OverviewController(GsiService gsiService) {
        this.gsiService = gsiService;
    }

    @FXML
    private void initialize() {
        configureTable(ctTable);
        configureTable(tTable);
        coupleSelection(ctTable, tTable);
        coupleSelection(tTable, ctTable);
        showDetail(null);

        // Tick the phase countdown locally so it counts down smoothly between GSI updates.
        Timeline ticker = new Timeline(new KeyFrame(Duration.millis(250), e -> updateTimeLabel()));
        ticker.setCycleCount(Animation.INDEFINITE);
        ticker.play();

        gsiService.onGameState(state -> Platform.runLater(() -> render(state)));
    }

    // ---- rendering -------------------------------------------------------------------------

    private void render(GameState state) {
        if (state == null) {
            return;
        }
        renderHeader(state);

        if (state.allPlayers.isEmpty()) {
            renderPlaceholder(state);
            return;
        }
        localSteamId = state.player.steamId;
        setScoreboardVisible(true);

        ctTable.getItems().setAll(playersOf(state, PlayerTeam.CT));
        tTable.getItems().setAll(playersOf(state, PlayerTeam.T));
        reselect(ctTable);
        reselect(tTable);
    }

    private void renderHeader(GameState state) {
        String map = state.map.name.isBlank() ? "-" : state.map.name;
        String mode = modeText(state.map.mode);
        mapLabel.setText(mode.isEmpty() ? map : map + "  ·  " + mode);
        ctScoreLabel.setText(scoreText(state.map.ctStatistics.score));
        tScoreLabel.setText(scoreText(state.map.tStatistics.score));

        String bomb = bombText(state);
        bombLabel.setText(bomb);
        bombLabel.setVisible(!bomb.isEmpty());
        bombLabel.setManaged(!bomb.isEmpty());

        updateRoundTimer(state);
    }

    // ---- round / bomb timer ----------------------------------------------------------------

    /** Standard competitive round time (1:55). */
    private static final long COMPETITIVE_ROUND_SECONDS = 115;
    /** Wingman (2v2) round time (1:30). */
    private static final long WINGMAN_ROUND_SECONDS = 90;
    /** C4 detonation time. */
    private static final long BOMB_SECONDS = 40;

    /**
     * CS2 does not send {@code phase_ends_in} in normal matches, so the timer is derived from state
     * transitions: the round going live starts a 1:55 countdown, a bomb plant starts a 40s countdown
     * (it takes over, as the round is still live), and the timer stops as soon as the round is no
     * longer live (bomb exploded/defused, round over, back in freezetime).
     */
    private void updateRoundTimer(GameState state) {
        Phase roundPhase = state.round.phase;
        BombState bombState = state.round.bombState;

        if (bombState == BombState.Planted && prevBomb != BombState.Planted) {
            startTimer(BOMB_SECONDS, true); // bomb just planted → detonation countdown
        } else if (roundPhase == Phase.Live && prevPhase != Phase.Live) {
            // Wingman (2v2) rounds are 1:30, everything else 1:55.
            long roundSeconds = state.map.mode == GameMode.Scrimcomp2v2 ? WINGMAN_ROUND_SECONDS : COMPETITIVE_ROUND_SECONDS;
            startTimer(roundSeconds, false); // round just went live
        }
        if (roundPhase != Phase.Live) {
            deadlineMillis = -1; // round no longer live → stop
        }

        prevPhase = roundPhase;
        prevBomb = bombState;
        updateTimeLabel();
    }

    private void startTimer(long seconds, boolean bomb) {
        deadlineMillis = System.currentTimeMillis() + seconds * 1000L;
        bombTimer = bomb;
    }

    private void updateTimeLabel() {
        if (deadlineMillis < 0) {
            timeLabel.setVisible(false);
            timeLabel.setManaged(false);
            return;
        }
        double remaining = Math.max(0, (deadlineMillis - System.currentTimeMillis()) / 1000.0);
        timeLabel.setText((bombTimer ? "Bombe  " : "") + formatClock(remaining));
        timeLabel.getStyleClass().remove("time-bomb");
        if (bombTimer) {
            timeLabel.getStyleClass().add("time-bomb");
        }
        timeLabel.setVisible(true);
        timeLabel.setManaged(true);
    }

    private static String formatClock(double seconds) {
        int total = (int) Math.ceil(seconds);
        return total / 60 + ":" + String.format(Locale.US, "%02d", total % 60);
    }

    private void renderPlaceholder(GameState state) {
        setScoreboardVisible(false);
        placeholderLabel.setText(
                "Kein Scoreboard verfügbar - CS2 sendet die Spielerliste nur im Spectator-/Observer-Modus "
                        + "(GOTV, Demo oder Beobachter). Es wird stattdessen der eigene Spieler angezeigt.");
        renderPlayerDetail(state.player, ownPlayerContent);
    }

    private void setScoreboardVisible(boolean scoreboard) {
        scoreboardBox.setVisible(scoreboard);
        scoreboardBox.setManaged(scoreboard);
        placeholderBox.setVisible(!scoreboard);
        placeholderBox.setManaged(!scoreboard);
    }

    private static List<Player> playersOf(GameState state, PlayerTeam team) {
        return state.allPlayers.values().stream()
                .filter(p -> p.team == team)
                .sorted(Comparator.comparingInt((Player p) -> p.matchStats.score).reversed()
                        .thenComparing(p -> p.name))
                .toList();
    }

    // ---- table wiring ----------------------------------------------------------------------

    private void configureTable(TableView<Player> table) {
        table.getColumns().setAll(List.of(
                column("Name", 150, p -> p.name.isBlank() ? "-" : p.name),
                column("HP", 44, p -> num(p.state.health)),
                column("Armor", 54, p -> num(p.state.armor)),
                column("$", 62, p -> money(p.state.money)),
                column("K", 34, p -> num(p.matchStats.kills)),
                column("A", 34, p -> num(p.matchStats.assists)),
                column("D", 34, p -> num(p.matchStats.deaths)),
                column("MVP", 44, p -> num(p.matchStats.mvps)),
                column("Score", 52, p -> num(p.matchStats.score)),
                column("Waffe", 130, p -> weaponName(p.getActiveWeapon()))));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setPlaceholder(new Label("Keine Spieler."));
        table.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Player player, boolean empty) {
                super.updateItem(player, empty);
                getStyleClass().removeAll("dead", "self");
                if (!empty && player != null) {
                    if (player.state.isValid() && player.state.health == 0) {
                        getStyleClass().add("dead");
                    }
                    if (localSteamId != null && localSteamId.equals(player.steamId)) {
                        getStyleClass().add("self");
                    }
                }
            }
        });
    }

    private static TableColumn<Player, String> column(String title, double width, Function<Player, String> value) {
        TableColumn<Player, String> col = new TableColumn<>(title);
        col.setPrefWidth(width);
        col.setSortable(false);
        col.setReorderable(false);
        col.setCellValueFactory(cd -> new SimpleStringProperty(value.apply(cd.getValue())));
        return col;
    }

    /** When a row in {@code table} is picked, clear the other table and show the details. */
    private void coupleSelection(TableView<Player> table, TableView<Player> other) {
        table.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null) {
                other.getSelectionModel().clearSelection();
                showDetail(selected);
            }
        });
    }

    private void reselect(TableView<Player> table) {
        if (selectedSteamId == null) {
            return;
        }
        for (Player player : table.getItems()) {
            if (selectedSteamId.equals(player.steamId)) {
                table.getSelectionModel().select(player);
                return;
            }
        }
    }

    private void showDetail(Player player) {
        if (player == null) {
            detailContent.getChildren().setAll(new Label("Spieler auswählen, um Details zu sehen."));
            return;
        }
        selectedSteamId = player.steamId;
        renderPlayerDetail(player, detailContent);
    }

    // ---- player detail card ----------------------------------------------------------------

    private void renderPlayerDetail(Player player, Pane target) {
        target.getChildren().clear();
        if (player == null || !player.isValid()) {
            target.getChildren().add(new Label("Keine Daten."));
            return;
        }
        String subtitle = teamText(player.team) + "  ·  " + activityText(player.activity);
        target.getChildren().add(title(player.name.isBlank() ? "Spieler" : player.name, subtitle));

        target.getChildren().add(bar("HP", player.state.health));
        target.getChildren().add(bar("Armor", player.state.armor));
        PlayerState state = player.state;
        target.getChildren().add(kv("Helm", yesNo(state.hasHelmet)));
        target.getChildren().add(kv("Defuse-Kit", yesNo(state.hasDefuseKit)));
        addMoney(target, "Geld", state.money);
        if (state.flashAmount > 0) {
            target.getChildren().add(kv("Geblendet", state.flashAmount + "/255"));
        }
        if (state.burningAmount > 0) {
            target.getChildren().add(kv("Brennt", state.burningAmount + "/255"));
        }

        // Round stats only if CS2 actually sent them - CS2 (Source 2) often omits round_totaldmg
        // and reports -1; showing an empty section or "–" just looks broken.
        if (state.roundKills >= 0 || state.roundHSKills >= 0
                || state.roundTotalDamage >= 0 || state.equipmentValue >= 0) {
            target.getChildren().add(section("Diese Runde"));
            addNum(target, "Kills", state.roundKills);
            addNum(target, "davon Headshots", state.roundHSKills);
            addNum(target, "Schaden", state.roundTotalDamage);
            addMoney(target, "Equipment-Wert", state.equipmentValue);
        }

        target.getChildren().add(section("Match"));
        target.getChildren().add(kv("K / A / D", num(player.matchStats.kills) + " / "
                + num(player.matchStats.assists) + " / " + num(player.matchStats.deaths)));
        addNum(target, "MVPs", player.matchStats.mvps);
        addNum(target, "Score", player.matchStats.score);

        target.getChildren().add(section("Waffen"));
        if (player.weapons.isEmpty()) {
            target.getChildren().add(new Label("-"));
        } else {
            for (Weapon weapon : player.weapons) {
                target.getChildren().add(new Label(weaponLine(weapon)));
            }
        }
    }

    // ---- small view helpers ----------------------------------------------------------------

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
            target.getChildren().add(kv(key, money(value)));
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

    // ---- value formatting ------------------------------------------------------------------

    /** Sentinel-aware integer: the library returns -1 for absent numeric fields. */
    private static String num(int value) {
        return value < 0 ? "–" : String.valueOf(value);
    }

    private static String money(int value) {
        return value < 0 ? "–" : "$" + value;
    }

    private static String scoreText(int value) {
        return value < 0 ? "0" : String.valueOf(value);
    }

    private static String yesNo(boolean value) {
        return value ? "ja" : "nein";
    }

    private static String weaponName(Weapon weapon) {
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

    private static String weaponLine(Weapon weapon) {
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
        if (weapon.state != com.cs2gsi.nodes.WeaponState.Undefined) {
            sb.append("  ·  ").append(weapon.state);
        }
        return sb.toString();
    }

    private static String teamText(PlayerTeam team) {
        return switch (team) {
            case CT -> "Counter-Terrorists";
            case T -> "Terrorists";
            case Spectator -> "Zuschauer";
            case Undefined -> "-";
        };
    }

    private static String activityText(PlayerActivity activity) {
        return switch (activity) {
            case Playing -> "spielt";
            case Menu -> "im Menü";
            case TextInput -> "tippt";
            case Undefined -> "-";
        };
    }

    private static String bombText(GameState state) {
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

    private static String modeText(GameMode mode) {
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
