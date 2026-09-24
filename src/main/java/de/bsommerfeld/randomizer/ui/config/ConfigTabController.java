package de.bsommerfeld.randomizer.ui.config;

import de.bsommerfeld.randomizer.config.ConfigRepository;
import de.bsommerfeld.randomizer.config.keybinds.KeybindConfig;
import de.bsommerfeld.randomizer.ui.FileChoosers;
import de.bsommerfeld.randomizer.vdf.VdfParseException;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Optional;

/**
 * One keybind-config tab. Shows the config as JSON and lets the user pick the file by hand. There
 * is one instance per keybind file, and the repository handed to {@link #init} decides which.
 */
public final class ConfigTabController {

    @FXML private Label statusLabel;
    @FXML private TextArea jsonArea;
    @FXML private TextField pathField;

    private ConfigRepository repository;
    private KeybindConfig current;

    /**
     * Binds this tab to its config. The main controller calls it after the FXML is loaded.
     *
     * <p>Not a constructor argument, because the FXML loader asks for a controller by class only.
     * Telling the two tabs apart at construction would mean counting instances in FXML order, which
     * breaks as soon as someone reorders the tabs.
     */
    public void init(ConfigRepository repository) {
        this.repository = repository;
        repository.loadOnStartup().ifPresentOrElse(this::show, this::showMissing);
    }

    /** The config this tab shows right now, or empty if none could be loaded. */
    public Optional<KeybindConfig> current() {
        return Optional.ofNullable(current);
    }

    private void show(KeybindConfig config) {
        current = config;
        statusLabel.setText("Config: " + config.source());
        jsonArea.setText(config.prettyJson());
    }

    private void showMissing() {
        current = null;
        statusLabel.setText("Config not found. Please select " + repository.source().fileName() + " manually below.");
        jsonArea.clear();
    }

    /** Runs auto-detection again and ignores any remembered path. */
    @FXML
    private void onReload() {
        repository.redetect().ifPresentOrElse(this::show, this::showMissing);
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
        String text = pathField.getText() == null ? "" : pathField.getText().trim();
        Optional<String> problem = problemWith(text, repository.source().fileName());
        if (problem.isPresent()) {
            statusLabel.setText(problem.get());
            return;
        }
        try {
            show(repository.loadAndRemember(Path.of(text)));
        } catch (IOException | VdfParseException e) {
            statusLabel.setText("Could not load: " + e.getMessage());
        }
    }

    /**
     * Why the typed {@code path} cannot be loaded into a tab that only takes {@code fileName},
     * as the message to show, or empty when it names an existing file of that name.
     */
    static Optional<String> problemWith(String path, String fileName) {
        if (path.isEmpty()) {
            return Optional.of("Please enter a path.");
        }
        Path file;
        try {
            file = Path.of(path);
        } catch (InvalidPathException e) {
            return Optional.of("Invalid path: " + path);
        }
        if (!Files.isRegularFile(file)) {
            return Optional.of("File not found: " + file);
        }
        if (!file.getFileName().toString().equalsIgnoreCase(fileName)) {
            return Optional.of("This tab only takes " + fileName + ".");
        }
        return Optional.empty();
    }
}
