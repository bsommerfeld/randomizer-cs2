package de.bsommerfeld.randomizer.ui.overview;

import com.cs2gsi.GameState;
import com.cs2gsi.nodes.Player;
import com.cs2gsi.nodes.PlayerTeam;
import de.bsommerfeld.randomizer.gsi.GsiService;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Drives the live "Übersicht" tab: a team-grouped scoreboard of all players plus a detail panel for
 * the selected one, fed by the structured {@link GameState} stream from {@link GsiService}. The
 * pieces are atomic collaborators: {@link RoundTimer} (countdown logic), {@link ScoreboardTable}
 * (table setup), {@link PlayerDetailView} (detail card) and {@link GameTexts} (formatting).
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
    private final RoundTimer roundTimer = new RoundTimer();

    /** Steam ID of the row whose details are shown, so selection survives item replacement. */
    private String selectedSteamId;
    /** Steam ID of the local player, used to highlight their scoreboard row. */
    private String localSteamId;

    public OverviewController(GsiService gsiService) {
        this.gsiService = gsiService;
    }

    @FXML
    private void initialize() {
        ScoreboardTable.configure(ctTable, () -> localSteamId);
        ScoreboardTable.configure(tTable, () -> localSteamId);
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

        ctTable.getItems().setAll(ScoreboardTable.playersOf(state, PlayerTeam.CT));
        tTable.getItems().setAll(ScoreboardTable.playersOf(state, PlayerTeam.T));
        reselect(ctTable);
        reselect(tTable);
    }

    private void renderHeader(GameState state) {
        String map = state.map.name.isBlank() ? "-" : state.map.name;
        String mode = GameTexts.mode(state.map.mode);
        mapLabel.setText(mode.isEmpty() ? map : map + "  ·  " + mode);
        ctScoreLabel.setText(GameTexts.score(state.map.ctStatistics.score));
        tScoreLabel.setText(GameTexts.score(state.map.tStatistics.score));

        String bomb = GameTexts.bomb(state);
        bombLabel.setText(bomb);
        bombLabel.setVisible(!bomb.isEmpty());
        bombLabel.setManaged(!bomb.isEmpty());

        roundTimer.update(state.round.phase, state.round.bombState, state.map.mode);
        updateTimeLabel();
    }

    private void updateTimeLabel() {
        roundTimer.remaining().ifPresentOrElse(remaining -> {
            timeLabel.setText((remaining.bomb() ? "Bombe  " : "") + GameTexts.clock(remaining.seconds()));
            timeLabel.getStyleClass().remove("time-bomb");
            if (remaining.bomb()) {
                timeLabel.getStyleClass().add("time-bomb");
            }
            timeLabel.setVisible(true);
            timeLabel.setManaged(true);
        }, () -> {
            timeLabel.setVisible(false);
            timeLabel.setManaged(false);
        });
    }

    private void renderPlaceholder(GameState state) {
        setScoreboardVisible(false);
        placeholderLabel.setText(
                "Kein Scoreboard verfügbar - CS2 sendet die Spielerliste nur im Spectator-/Observer-Modus "
                        + "(GOTV, Demo oder Beobachter). Es wird stattdessen der eigene Spieler angezeigt.");
        PlayerDetailView.render(state.player, ownPlayerContent);
    }

    private void setScoreboardVisible(boolean scoreboard) {
        scoreboardBox.setVisible(scoreboard);
        scoreboardBox.setManaged(scoreboard);
        placeholderBox.setVisible(!scoreboard);
        placeholderBox.setManaged(!scoreboard);
    }

    // ---- selection ---------------------------------------------------------------------------

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
        PlayerDetailView.render(player, detailContent);
    }
}
