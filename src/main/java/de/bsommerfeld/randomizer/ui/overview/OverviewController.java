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
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.Optional;

/**
 * The live overview tab. Shows a scoreboard per team and the details of the selected player.
 *
 * <p>CS2 fills {@code allPlayers} only for spectators and observers. In a normal match it is empty,
 * and the tab shows a hint and the local player's own card instead.
 */
public final class OverviewController {

    @FXML private Label mapLabel;
    @FXML private Label modeLabel;
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
    @FXML private Node ownPlayerCard;
    @FXML private VBox ownPlayerContent;

    private final GsiService gsiService;
    private final RoundTimer roundTimer = new RoundTimer();

    /** Kept by Steam ID, so the selection survives each game state replacing the rows. */
    private String selectedSteamId;
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
        startCountdownTicker();
        gsiService.onGameState(state -> Platform.runLater(() -> render(state)));
    }

    /** Ticks locally, so the countdown keeps moving between two game states. */
    private void startCountdownTicker() {
        Timeline ticker = new Timeline(new KeyFrame(Duration.millis(250), e -> renderCountdown()));
        ticker.setCycleCount(Animation.INDEFINITE);
        ticker.play();
    }

    // ---- rendering -------------------------------------------------------------------------

    private void render(GameState state) {
        if (state == null) {
            return;
        }
        roundTimer.update(state.round.phase, state.round.bombState, state.map.mode);
        renderHeader(state);
        renderCountdown();
        if (state.allPlayers.isEmpty()) {
            renderOwnPlayer(state);
        } else {
            renderScoreboard(state);
        }
    }

    private void renderHeader(GameState state) {
        mapLabel.setText(state.map.name.isBlank() ? GameTexts.ABSENT : state.map.name);
        modeLabel.setText(state.map.mode.displayName);
        ctScoreLabel.setText(GameTexts.score(state.map.ctStatistics.score));
        tScoreLabel.setText(GameTexts.score(state.map.tStatistics.score));

        String bomb = GameTexts.bomb(state.round.bombState, state.bomb);
        bombLabel.setText(bomb);
        setShown(bombLabel, !bomb.isEmpty());
    }

    private void renderCountdown() {
        Optional<RoundTimer.Remaining> remaining = roundTimer.remaining();
        setShown(timeLabel, remaining.isPresent());
        remaining.ifPresent(countdown -> {
            timeLabel.setText(GameTexts.countdown(countdown));
            timeLabel.getStyleClass().remove("time-bomb");
            if (countdown.bomb()) {
                timeLabel.getStyleClass().add("time-bomb");
            }
        });
    }

    private void renderScoreboard(GameState state) {
        localSteamId = state.player.steamId;
        showScoreboard(true);
        ctTable.getItems().setAll(ScoreboardTable.playersOf(state, PlayerTeam.CT));
        tTable.getItems().setAll(ScoreboardTable.playersOf(state, PlayerTeam.T));
        reselect(ctTable);
        reselect(tTable);
    }

    /** CS2 sent no player list, so only the local player is known. */
    private void renderOwnPlayer(GameState state) {
        showScoreboard(false);
        placeholderLabel.setText(
                "CS2 sends the scoreboard only to spectators (GOTV, demo, observer). "
                        + "Here you see your own player.");
        PlayerDetailView.render(state.player, ownPlayerContent);
        setShown(ownPlayerCard, true);
    }

    private void showScoreboard(boolean scoreboard) {
        setShown(scoreboardBox, scoreboard);
        setShown(placeholderBox, !scoreboard);
    }

    /** Hidden nodes also give up their space in the layout. */
    private static void setShown(Node node, boolean shown) {
        node.setVisible(shown);
        node.setManaged(shown);
    }

    // ---- selection ---------------------------------------------------------------------------

    /** A row picked in {@code table} clears the selection in {@code other} and shows its details. */
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
            detailContent.getChildren().setAll(new Label("Select a player to see their details."));
            return;
        }
        selectedSteamId = player.steamId;
        PlayerDetailView.render(player, detailContent);
    }
}
