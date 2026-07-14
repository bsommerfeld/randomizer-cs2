package de.bsommerfeld.randomizer.ui;

import de.bsommerfeld.randomizer.config.ConfigKind;
import de.bsommerfeld.randomizer.config.Cs2ConfigService;
import de.bsommerfeld.randomizer.config.Cs2ConfigService.LoadedConfig;
import de.bsommerfeld.randomizer.gsi.GsiService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

public class MainController {

    @FXML
    private TabPane tabPane;
    @FXML
    private Tab userTab;
    @FXML
    private Tab gsiTab;
    @FXML
    private Label defaultStatusLabel;
    @FXML
    private Label userStatusLabel;
    @FXML
    private Label gsiStatusLabel;
    @FXML
    private Button gsiToggleButton;
    @FXML
    private TextArea defaultJsonArea;
    @FXML
    private TextArea userJsonArea;
    @FXML
    private TextArea gsiJsonArea;
    @FXML
    private ListView<GsiService.GsiEvent> gsiEventsList;
    @FXML
    private TextArea gsiEventDetailsArea;
    @FXML
    private HBox manualBox;
    @FXML
    private TextField pathField;

    private final Cs2ConfigService configService;
    private final GsiService gsiService;

    public MainController(Cs2ConfigService configService, GsiService gsiService) {
        this.configService = configService;
        this.gsiService = gsiService;
    }

    @FXML
    private void initialize() {
        for (ConfigKind kind : ConfigKind.values()) {
            configService.loadConfigOnStartup(kind).ifPresentOrElse(
                    config -> showConfig(kind, config),
                    () -> showNotFound(kind));
        }
        // The manual path picker only applies to the two config tabs
        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            boolean configTab = newTab != gsiTab;
            manualBox.setVisible(configTab);
            manualBox.setManaged(configTab);
        });
        initGsi();
    }

    private static final int MAX_EVENT_LOG_ENTRIES = 1000;

    private void initGsi() {
        gsiEventsList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(GsiService.GsiEvent item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.summary());
            }
        });
        gsiEventsList.getSelectionModel().selectedItemProperty().addListener((obs, oldEvent, selected) ->
                gsiEventDetailsArea.setText(selected == null ? "" : selected.details()));

        gsiService.onGameStateJson(json -> Platform.runLater(() -> updateGsiJson(json)));
        gsiService.onGameEvent(event -> Platform.runLater(() -> appendEvent(event)));
        gsiStatusLabel.setText("GSI nicht gestartet — mit \"GSI starten\" beginnen, dann sendet CS2 seine Events hierher.");
    }

    @FXML
    private void onGsiToggle() {
        if (gsiService.isRunning()) {
            gsiService.stop();
            gsiToggleButton.setText("GSI starten");
            gsiStatusLabel.setText("GSI gestoppt.");
            return;
        }
        if (!gsiService.start()) {
            gsiStatusLabel.setText("GSI-Server konnte nicht gestartet werden — ist Port "
                    + gsiService.getPort() + " schon belegt?");
            return;
        }
        gsiToggleButton.setText("GSI stoppen");
        if (gsiService.generateConfigFile()) {
            gsiStatusLabel.setText("Warte auf Daten von CS2 (Port " + gsiService.getPort()
                    + ") — gamestate_integration_randomizer.cfg wurde erzeugt, ggf. CS2 neu starten.");
        } else {
            gsiStatusLabel.setText("GSI-Server läuft (Port " + gsiService.getPort()
                    + "), aber die gamestate_integration-Config konnte nicht erzeugt werden.");
        }
    }

    /** Replaces the game-state JSON without losing the TextArea's scroll position. */
    private void updateGsiJson(String json) {
        double scrollTop = gsiJsonArea.getScrollTop();
        double scrollLeft = gsiJsonArea.getScrollLeft();
        gsiJsonArea.setText(json);
        // Re-apply only after the layout pass, otherwise setText() overrides the position
        Platform.runLater(() -> {
            gsiJsonArea.setScrollTop(scrollTop);
            gsiJsonArea.setScrollLeft(scrollLeft);
        });
    }

    private void appendEvent(GsiService.GsiEvent event) {
        gsiEventsList.getItems().add(event);
        // Cap the log so the list does not grow unbounded during long sessions
        if (gsiEventsList.getItems().size() > MAX_EVENT_LOG_ENTRIES) {
            gsiEventsList.getItems().removeFirst();
        }
    }

    /** Re-runs automatic detection; any remembered path is ignored. */
    @FXML
    private void onReload() {
        for (ConfigKind kind : ConfigKind.values()) {
            configService.redetectAndLoad(kind).ifPresentOrElse(
                    config -> showConfig(kind, config),
                    () -> showNotFound(kind));
        }
    }

    private void showConfig(ConfigKind kind, LoadedConfig config) {
        statusLabel(kind).setText("Config: " + config.source());
        jsonArea(kind).setText(config.prettyJson());
    }

    private void showNotFound(ConfigKind kind) {
        statusLabel(kind).setText(
                "Config nicht gefunden — bitte " + kind.fileName() + " unten manuell auswählen.");
        jsonArea(kind).clear();
    }

    @FXML
    private void onBrowse() {
        ConfigKind kind = activeKind();
        FileChooser chooser = new FileChooser();
        chooser.setTitle(kind.fileName() + " auswählen");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(kind.fileName(), kind.fileName()));
        File file = chooser.showOpenDialog(pathField.getScene().getWindow());
        if (file != null) {
            pathField.setText(file.getAbsolutePath());
        }
    }

    @FXML
    private void onLoad() {
        ConfigKind kind = activeKind();
        String text = pathField.getText() == null ? "" : pathField.getText().trim();
        if (text.isEmpty()) {
            statusLabel(kind).setText("Bitte einen Pfad angeben.");
            return;
        }
        Path path;
        try {
            path = Path.of(text);
        } catch (InvalidPathException e) {
            statusLabel(kind).setText("Ungültiger Pfad: " + text);
            return;
        }
        if (!Files.isRegularFile(path)) {
            statusLabel(kind).setText("Datei nicht gefunden: " + path);
            return;
        }
        if (!path.getFileName().toString().equalsIgnoreCase(kind.fileName())) {
            statusLabel(kind).setText("Für diesen Tab ist nur " + kind.fileName() + " erlaubt.");
            return;
        }
        try {
            showConfig(kind, configService.loadAndRemember(kind, path));
        } catch (Exception e) {
            statusLabel(kind).setText("Fehler beim Laden: " + e.getMessage());
        }
    }

    private ConfigKind activeKind() {
        return tabPane.getSelectionModel().getSelectedItem() == userTab ? ConfigKind.USER : ConfigKind.DEFAULT;
    }

    private Label statusLabel(ConfigKind kind) {
        return kind == ConfigKind.USER ? userStatusLabel : defaultStatusLabel;
    }

    private TextArea jsonArea(ConfigKind kind) {
        return kind == ConfigKind.USER ? userJsonArea : defaultJsonArea;
    }
}
