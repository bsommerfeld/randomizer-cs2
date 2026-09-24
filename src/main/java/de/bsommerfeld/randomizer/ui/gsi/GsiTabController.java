package de.bsommerfeld.randomizer.ui.gsi;

import com.cs2gsi.GSIConfigResult;
import com.cs2gsi.GSIConfigResult.Status;
import de.bsommerfeld.randomizer.gsi.GsiEvent;
import de.bsommerfeld.randomizer.gsi.GsiService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.IndexedCell;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.skin.VirtualFlow;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import java.nio.file.Path;
import java.util.function.BooleanSupplier;

/**
 * The "Live (GSI)" tab. Starts and stops the GSI listener, shows the current game state as JSON and
 * keeps a capped log of the incoming events with a detail view.
 */
public final class GsiTabController {

    private static final int MAX_EVENT_LOG_ENTRIES = 1000;

    @FXML private Label statusLabel;
    @FXML private Button toggleButton;
    @FXML private TextArea jsonArea;
    @FXML private ListView<GsiEvent> eventsList;
    @FXML private TextArea eventDetailsArea;

    private final GsiService gsiService;
    private final BooleanSupplier cs2Running;

    public GsiTabController(GsiService gsiService, BooleanSupplier cs2Running) {
        this.gsiService = gsiService;
        this.cs2Running = cs2Running;
    }

    @FXML
    private void initialize() {
        eventsList.setCellFactory(list -> new SummaryCell());
        eventsList.getSelectionModel().selectedItemProperty().addListener((obs, oldEvent, selected) ->
                eventDetailsArea.setText(selected == null ? "" : selected.details()));
        eventsList.addEventHandler(KeyEvent.KEY_PRESSED, this::deselectOnEscape);

        gsiService.onGameStateJson(json -> Platform.runLater(() -> updateJson(json)));
        gsiService.onGameEvent(event -> Platform.runLater(() -> appendEvent(event)));
        statusLabel.setText("GSI is not running. Press \"Start GSI\" and CS2 sends its events here.");
    }

    private void deselectOnEscape(KeyEvent key) {
        if (key.getCode() == KeyCode.ESCAPE) {
            eventsList.getSelectionModel().clearSelection();
        }
    }

    /** Shows an event's summary. Its details show up in the area below once selected. */
    private static final class SummaryCell extends ListCell<GsiEvent> {
        @Override
        protected void updateItem(GsiEvent item, boolean empty) {
            super.updateItem(item, empty);
            setText(empty || item == null ? null : item.summary());
        }
    }

    @FXML
    private void onToggle() {
        if (gsiService.isRunning()) {
            stop();
        } else {
            start();
        }
    }

    /**
     * Starts GSI unless it already runs, for the randomizer, which cannot fire without game state.
     * Returns false when it could not start. This tab's status line then says why.
     */
    public boolean ensureStarted() {
        if (!gsiService.isRunning()) {
            start();
        }
        return gsiService.isRunning();
    }

    private void stop() {
        gsiService.stop();
        toggleButton.setText("Start GSI");
        statusLabel.setText("GSI stopped.");
    }

    /** Starts the listener and installs the GSI config, reporting the first thing that goes wrong. */
    private void start() {
        if (!cs2Running.getAsBoolean()) {
            statusLabel.setText("CS2 is not running. Start CS2 first, then switch GSI on.");
            return;
        }
        if (!gsiService.start()) {
            statusLabel.setText("The GSI server could not start. Is port " + gsiService.getPort() + " already in use?");
            return;
        }
        toggleButton.setText("Stop GSI");
        GSIConfigResult install = gsiService.installConfigFile();
        statusLabel.setText(statusAfter(install.status(), gsiService.getPort()));
        if (install.status() == Status.CREATED || install.status() == Status.UPDATED) {
            showRestartNotice(install.file());
        }
    }

    private static String statusAfter(Status install, int port) {
        return switch (install) {
            case CREATED, UPDATED -> "GSI is running (port " + port + "). CS2 needs one restart "
                    + "before data comes in.";
            case UNCHANGED -> "Waiting for data from CS2 (port " + port + ").";
            case FAILED -> "The GSI server is running (port " + port
                    + "), but the gamestate_integration config could not be written.";
        };
    }

    /** Pops up over the app window, in the app's own look. */
    private void showRestartNotice(Path configFile) {
        Alert notice = restartNotice(configFile);
        notice.initOwner(toggleButton.getScene().getWindow());
        notice.getDialogPane().getStylesheets().setAll(toggleButton.getScene().getStylesheets());
        notice.show();
    }

    private static Alert restartNotice(Path configFile) {
        Alert notice = new Alert(Alert.AlertType.INFORMATION);
        notice.setTitle("Randomizer CS2");
        notice.setGraphic(null);
        notice.setHeaderText("Restart CS2 once");
        notice.setContentText("The file " + configFile.getFileName() + " was just written to the CS2 cfg "
                + "folder. CS2 reads such files only on startup.\n\n"
                + "Quit CS2 and start it again, then data comes in here. GSI can keep running meanwhile.");
        return notice;
    }

    /** Replaces the game-state JSON without losing the TextArea's scroll position. */
    private void updateJson(String json) {
        double scrollTop = jsonArea.getScrollTop();
        double scrollLeft = jsonArea.getScrollLeft();
        jsonArea.setText(json);
        // Restored only after the layout pass, otherwise the setText() above overrides it
        Platform.runLater(() -> {
            jsonArea.setScrollTop(scrollTop);
            jsonArea.setScrollLeft(scrollLeft);
        });
    }

    /**
     * The newest event goes to the bottom. Rows appended below leave the rows in view where they are,
     * so reading older events needs no help. The list follows the game while its last row is in view.
     * A selection does not hold it, scrolling up does. The selected event stays in the details area.
     *
     * <p>ponytail: once the log is full, every event drops the top row and the rows in view move up
     * by one. Hold the view with the first visible cell's index and offset if that bothers anyone.
     */
    private void appendEvent(GsiEvent event) {
        VirtualFlow<?> flow = (VirtualFlow<?>) eventsList.lookup(".virtual-flow");
        boolean following = lastRowInView(flow);
        eventsList.getItems().add(event);
        capEventLog();
        if (following && flow != null) {
            // The flow learns of the new row only in a layout pass
            eventsList.layout();
            // Scrolls by the new row's height. ListView.scrollTo() goes through the flow's estimated total height,
            // lands a row short once that drifts, and the next event then no longer sees the last row
            flow.scrollTo(eventsList.getItems().size() - 1);
        }
    }

    /** Until the tab is shown for the first time the list has no viewport, which counts as being at the end. */
    private boolean lastRowInView(VirtualFlow<?> flow) {
        IndexedCell<?> lastVisible = flow == null ? null : flow.getLastVisibleCell();
        return lastVisible == null || lastVisible.getIndex() >= eventsList.getItems().size() - 1;
    }

    /** Drops the oldest event once the log is full. */
    private void capEventLog() {
        if (eventsList.getItems().size() <= MAX_EVENT_LOG_ENTRIES) {
            return;
        }
        // JavaFX hands the selection of a removed row to its neighbour, here to every next oldest event in turn
        if (eventsList.getSelectionModel().getSelectedIndex() == 0) {
            eventsList.getSelectionModel().clearSelection();
        }
        eventsList.getItems().removeFirst();
    }
}
