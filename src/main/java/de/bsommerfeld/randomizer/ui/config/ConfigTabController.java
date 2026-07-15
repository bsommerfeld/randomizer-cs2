package de.bsommerfeld.randomizer.ui.config;

import de.bsommerfeld.randomizer.config.ConfigRepository;
import de.bsommerfeld.randomizer.config.keybinds.KeybindConfig;
import de.bsommerfeld.randomizer.ui.FileChoosers;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Drives one keybind-config tab: shows the config as pretty JSON plus a manual picker for the one
 * file name its {@link ConfigRepository} accepts. The same FXML is instantiated once per
 * {@link de.bsommerfeld.randomizer.config.keybinds.ConfigKind}; which config it shows is decided
 * solely by the repository handed to {@link #init}.
 */
public final class ConfigTabController {

    @FXML private Label statusLabel;
    @FXML private TextArea jsonArea;
    @FXML private TextField pathField;

    private ConfigRepository<KeybindConfig> repository;

    /** Binds this tab to its config; called by the main controller after the FXML is loaded. */
    public void init(ConfigRepository<KeybindConfig> repository) {
        this.repository = repository;
        show(repository.loadOnStartup());
    }

    private void show(Optional<KeybindConfig> loaded) {
        loaded.ifPresentOrElse(config -> {
            statusLabel.setText("Config: " + config.source());
            jsonArea.setText(config.prettyJson());
        }, () -> {
            statusLabel.setText("Config nicht gefunden - bitte "
                    + repository.source().fileName() + " unten manuell auswählen.");
            jsonArea.clear();
        });
    }

    /** Re-runs automatic detection; any remembered path is ignored. */
    @FXML
    private void onReload() {
        show(repository.redetect());
    }

    @FXML
    private void onBrowse() {
        File file = FileChoosers.exactName(repository.source().fileName())
                .showOpenDialog(pathField.getScene().getWindow());
        if (file != null) {
            pathField.setText(file.getAbsolutePath());
        }
    }

    @FXML
    private void onLoad() {
        String fileName = repository.source().fileName();
        String text = pathField.getText() == null ? "" : pathField.getText().trim();
        if (text.isEmpty()) {
            statusLabel.setText("Bitte einen Pfad angeben.");
            return;
        }
        Path path;
        try {
            path = Path.of(text);
        } catch (InvalidPathException e) {
            statusLabel.setText("Ungültiger Pfad: " + text);
            return;
        }
        if (!Files.isRegularFile(path)) {
            statusLabel.setText("Datei nicht gefunden: " + path);
            return;
        }
        if (!path.getFileName().toString().equalsIgnoreCase(fileName)) {
            statusLabel.setText("Für diesen Tab ist nur " + fileName + " erlaubt.");
            return;
        }
        try {
            show(Optional.of(repository.loadAndRemember(path)));
        } catch (Exception e) {
            statusLabel.setText("Fehler beim Laden: " + e.getMessage());
        }
    }
}
