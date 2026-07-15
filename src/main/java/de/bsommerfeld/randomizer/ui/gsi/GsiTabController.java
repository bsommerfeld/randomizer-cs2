package de.bsommerfeld.randomizer.ui.gsi;

import de.bsommerfeld.randomizer.gsi.GsiEvent;
import de.bsommerfeld.randomizer.gsi.GsiService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;

/**
 * Drives the "Live (GSI)" tab: starts/stops the local GSI listener, shows the current game state
 * as JSON and keeps a capped log of the incoming events with a detail view.
 *
 * <p>GSI callbacks arrive on the listener thread and are marshalled onto the JavaFX thread via
 * {@link Platform#runLater} before touching any control.
 */
public final class GsiTabController {

    private static final int MAX_EVENT_LOG_ENTRIES = 1000;

    @FXML private Label statusLabel;
    @FXML private Button toggleButton;
    @FXML private TextArea jsonArea;
    @FXML private ListView<GsiEvent> eventsList;
    @FXML private TextArea eventDetailsArea;

    private final GsiService gsiService;

    public GsiTabController(GsiService gsiService) {
        this.gsiService = gsiService;
    }

    @FXML
    private void initialize() {
        eventsList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(GsiEvent item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.summary());
            }
        });
        eventsList.getSelectionModel().selectedItemProperty().addListener((obs, oldEvent, selected) ->
                eventDetailsArea.setText(selected == null ? "" : selected.details()));

        gsiService.onGameStateJson(json -> Platform.runLater(() -> updateJson(json)));
        gsiService.onGameEvent(event -> Platform.runLater(() -> appendEvent(event)));
        statusLabel.setText("GSI nicht gestartet - mit \"GSI starten\" beginnen, dann sendet CS2 seine Events hierher.");
    }

    @FXML
    private void onToggle() {
        if (gsiService.isRunning()) {
            gsiService.stop();
            toggleButton.setText("GSI starten");
            statusLabel.setText("GSI gestoppt.");
            return;
        }
        if (!gsiService.start()) {
            statusLabel.setText("GSI-Server konnte nicht gestartet werden - ist Port "
                    + gsiService.getPort() + " schon belegt?");
            return;
        }
        toggleButton.setText("GSI stoppen");
        if (gsiService.generateConfigFile()) {
            statusLabel.setText("Warte auf Daten von CS2 (Port " + gsiService.getPort()
                    + ") - gamestate_integration_randomizer.cfg wurde erzeugt, ggf. CS2 neu starten.");
        } else {
            statusLabel.setText("GSI-Server läuft (Port " + gsiService.getPort()
                    + "), aber die gamestate_integration-Config konnte nicht erzeugt werden.");
        }
    }

    /** Replaces the game-state JSON without losing the TextArea's scroll position. */
    private void updateJson(String json) {
        double scrollTop = jsonArea.getScrollTop();
        double scrollLeft = jsonArea.getScrollLeft();
        jsonArea.setText(json);
        // Re-apply only after the layout pass, otherwise setText() overrides the position
        Platform.runLater(() -> {
            jsonArea.setScrollTop(scrollTop);
            jsonArea.setScrollLeft(scrollLeft);
        });
    }

    private void appendEvent(GsiEvent event) {
        eventsList.getItems().add(event);
        // Cap the log so the list does not grow unbounded during long sessions
        if (eventsList.getItems().size() > MAX_EVENT_LOG_ENTRIES) {
            eventsList.getItems().removeFirst();
        }
    }
}
