package de.bsommerfeld.randomizer.ui.crosshair;

import de.bsommerfeld.randomizer.config.AppPreferences;
import de.bsommerfeld.randomizer.config.ConfigRepository;
import de.bsommerfeld.randomizer.config.ConfigSaver;
import de.bsommerfeld.randomizer.config.crosshair.Crosshair;
import de.bsommerfeld.randomizer.config.crosshair.CrosshairConvar;
import de.bsommerfeld.randomizer.config.crosshair.CrosshairRandomizer;
import de.bsommerfeld.randomizer.config.crosshair.CrosshairSettings;
import de.bsommerfeld.randomizer.config.crosshair.CrosshairStandard;
import de.bsommerfeld.randomizer.exec.ExecApplier;
import de.bsommerfeld.randomizer.exec.ExecConfig;
import de.bsommerfeld.randomizer.exec.ExecKeys;
import de.bsommerfeld.randomizer.ui.FileChoosers;
import de.bsommerfeld.randomizer.vdf.VdfParseException;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Drives the "Fadenkreuz" tab: previews a crosshair over a switchable map background next to its
 * values. Editing works on an in-memory working copy restricted to the {@link CrosshairConvar}
 * catalogue - "Zufällig" fills it with random values, "Standard" restores the user's own standard
 * crosshair (its disk backup via {@link CrosshairStandard}), not CS2 defaults. "Speichern" writes
 * the working copy straight into the live .vcfg ({@link ConfigSaver} - only the catalogue's convars
 * change, everything else keeps its on-disk state) and additionally applies the values to a
 * running CS2 via {@link ExecApplier}: randomizer.cfg is written into the game's cfg folder and
 * the bound exec key is pressed on the CS2 window (one-time prerequisite: {@code bind l "exec
 * randomizer"} in the CS2 console). "Backup wiederherstellen" rolls the live file back byte-exactly
 * to the app-data backup and applies the restored values the same way. Map cycling lives in
 * {@link MapBackdrop}, pixel drawing in {@link CrosshairRenderer}.
 */
public final class CrosshairController {

    /**
     * Calibrated to the bundled 1440p screenshots: the crosshair is drawn at real 1440p pixels and
     * the background is shown 1:1 (crosshair pixels scale with vertical resolution, 1080p being 1.0).
     */
    private static final double SCALE = 1440.0 / 1080.0;

    @FXML private ImageView backgroundImage;
    @FXML private Canvas crosshairCanvas;
    @FXML private Label mapNameLabel;
    @FXML private Label statusLabel;
    @FXML private Pane valuesBox;
    @FXML private Button saveButton;
    @FXML private Button restoreBackupButton;
    @FXML private TextField execKeyField;
    @FXML private Label execKeyHint;
    @FXML private TextField bindCommandField;

    private final ConfigRepository<Crosshair> repository;
    private final CrosshairStandard standard;
    private final ExecApplier execApplier;
    private final AppPreferences preferences;
    private final CrosshairRandomizer randomizer = new CrosshairRandomizer();
    private final ConfigSaver saver = new ConfigSaver();

    private MapBackdrop backdrop;

    /** Path of the live .vcfg the current values came from; null when nothing was found/loaded. */
    private Path livePath;

    /**
     * Bumped by every {@link #setStatus}; an async exec-apply result only writes its status if the
     * generation it captured is still current, so it never clobbers a newer message. FX thread only.
     */
    private int statusGeneration;

    /** The user's standard crosshair (canonical values from its disk backup), restored by "Standard". */
    private Map<String, String> standardValues = CrosshairConvar.defaults();
    /** The currently shown/edited values. */
    private Map<String, String> working = new LinkedHashMap<>(standardValues);
    /** Status text describing the loaded config. */
    private String loadedStatus = "";

    public CrosshairController(ConfigRepository<Crosshair> repository, CrosshairStandard standard,
                               ExecApplier execApplier, AppPreferences preferences) {
        this.repository = repository;
        this.standard = standard;
        this.execApplier = execApplier;
        this.preferences = preferences;
    }

    @FXML
    private void initialize() {
        backdrop = new MapBackdrop(backgroundImage, mapNameLabel,
                () -> crosshairCanvas.getWidth() / (1920.0 * SCALE));
        backdrop.showFirst();
        execKeyField.setText(execApplier.keyName());
        updateBindCommand();
        execKeyField.textProperty().addListener((observable, oldText, newText) -> onExecKeyChanged(newText));
        applyLoaded(repository.loadOnStartup(), false);
    }

    // ---- exec key ----------------------------------------------------------------------------

    private void onExecKeyChanged(String text) {
        if (ExecKeys.key(text).isEmpty()) {
            execKeyHint.setText("Ungültig - erlaubt: a-z, f1-f24");
            return;
        }
        try {
            preferences.setString(ExecApplier.KEY_PREFERENCE, text.trim().toLowerCase(Locale.ROOT));
            execKeyHint.setText("");
            updateBindCommand();
        } catch (IOException e) {
            execKeyHint.setText("Speichern fehlgeschlagen: " + e.getMessage());
        }
    }

    /** The console command matching the configured key, ready to copy into CS2. */
    private void updateBindCommand() {
        bindCommandField.setText("bind " + execApplier.keyName()
                + " \"exec " + ExecConfig.EXEC_NAME + "\"");
    }

    // ---- loading -----------------------------------------------------------------------------

    /**
     * @param explicit whether {@code loaded} came from the user explicitly picking a file (which
     *                 updates the standard backup); auto-loads only capture the backup if none exists
     */
    private void applyLoaded(Optional<Crosshair> loaded, boolean explicit) {
        standardValues = standard.resolve(loaded, explicit);
        livePath = loaded.map(Crosshair::source).orElse(null);
        if (loaded.isPresent()) {
            working = CrosshairConvar.canonicalValues(loaded.get().convars());
            loadedStatus = "Geladen: " + loaded.get().source();
        } else {
            working = new LinkedHashMap<>(standardValues);
            loadedStatus = standard.hasBackup()
                    ? "Datei nicht gefunden - gesichertes Standard-Fadenkreuz wird angezeigt."
                    : repository.source().fileName()
                            + " nicht gefunden - Standardwerte werden angezeigt, unten manuell wählbar.";
        }
        renderWorking();
        setStatus(loadedStatus);
        updateActionAvailability();
    }

    private void updateActionAvailability() {
        saveButton.setDisable(livePath == null);
        restoreBackupButton.setDisable(livePath == null || !standard.hasBackup());
    }

    // ---- status ------------------------------------------------------------------------------

    private void setStatus(String text) {
        statusGeneration++;
        statusLabel.setText(text);
    }

    /**
     * Applies {@code working} to a running CS2 (randomizer.cfg + exec-Tastendruck) on a virtual
     * thread; never fails the save. The async status starts with {@code prefix} and describes the
     * outcome - unless a newer status exists by then.
     */
    private void applyLive(String prefix) {
        Map<String, String> values = Map.copyOf(working);
        int generation = statusGeneration;
        Thread.startVirtualThread(() -> {
            String status;
            try {
                status = switch (execApplier.apply(values)) {
                    case TRIGGERED -> prefix + " und live an CS2 übertragen (exec "
                            + ExecConfig.EXEC_NAME + " per " + execApplier.keyName() + ").";
                    case CFG_ONLY_WINDOW_NOT_FOUND ->
                            prefix + ". CS2 läuft nicht - Werte gelten ab dem nächsten Start.";
                    case CFG_ONLY_FOCUS_DENIED -> prefix + ". Automatischer exec fehlgeschlagen - im"
                            + " Spiel \"exec " + ExecConfig.EXEC_NAME + "\" ausführen (Bind: bind "
                            + execApplier.keyName() + " \"exec " + ExecConfig.EXEC_NAME + "\").";
                    case CFG_FOLDER_MISSING -> prefix
                            + " - CS2-cfg-Ordner nicht gefunden; Werte gelten ab dem nächsten Start.";
                };
            } catch (IOException e) {
                status = prefix + " - " + ExecConfig.FILE_NAME
                        + " konnte nicht geschrieben werden: " + e.getMessage();
            }
            String finalStatus = status;
            Platform.runLater(() -> {
                if (generation == statusGeneration) {
                    statusLabel.setText(finalStatus);
                }
            });
        });
    }

    // ---- editing actions ---------------------------------------------------------------------

    @FXML
    private void onRandom() {
        working = randomizer.randomValues();
        renderWorking();
        setStatus("Zufälliges Fadenkreuz - mit \"Speichern\" übernehmen, \"Standard\" setzt zurück.");
    }

    @FXML
    private void onReset() {
        working = new LinkedHashMap<>(standardValues);
        renderWorking();
        setStatus("Standard-Fadenkreuz wiederhergestellt (noch nicht gespeichert).");
    }

    @FXML
    private void onSave() {
        if (livePath == null) {
            return;
        }
        try {
            saver.save(livePath, working);
            setStatus("Gespeichert: " + livePath + " - Übertrage an CS2…");
            applyLive("Gespeichert");
        } catch (IOException | VdfParseException e) {
            setStatus("Fehler beim Speichern: " + e.getMessage());
        }
    }

    @FXML
    private void onRestoreBackup() {
        Path target = livePath;
        if (target == null) {
            return;
        }
        try {
            standard.restoreTo(target);
            applyLoaded(Optional.of(repository.load(target)), false);
            setStatus("Backup wiederhergestellt: " + target + " - Übertrage an CS2…");
            applyLive("Backup wiederhergestellt");
        } catch (IOException | VdfParseException e) {
            setStatus("Fehler beim Wiederherstellen: " + e.getMessage());
        }
    }

    // ---- rendering ---------------------------------------------------------------------------

    private void renderWorking() {
        populateValues(working);
        drawCrosshair(CrosshairSettings.fromConvars(working));
    }

    private void populateValues(Map<String, String> values) {
        valuesBox.getChildren().clear();
        values.forEach((key, value) -> {
            Label row = new Label(key + " = " + value);
            row.getStyleClass().add("crosshair-value");
            valuesBox.getChildren().add(row);
        });
    }

    private void drawCrosshair(CrosshairSettings settings) {
        GraphicsContext g = crosshairCanvas.getGraphicsContext2D();
        g.clearRect(0, 0, crosshairCanvas.getWidth(), crosshairCanvas.getHeight());
        CrosshairRenderer.draw(g, crosshairCanvas.getWidth() / 2, crosshairCanvas.getHeight() / 2,
                settings, SCALE);
    }

    // ---- map / file actions ------------------------------------------------------------------

    @FXML
    private void onPrevMap() {
        backdrop.previous();
    }

    @FXML
    private void onNextMap() {
        backdrop.next();
    }

    @FXML
    private void onReload() {
        applyLoaded(repository.redetect(), false);
    }

    @FXML
    private void onChoose() {
        String fileName = repository.source().fileName();
        File file = FileChoosers.exactName(fileName)
                .showOpenDialog(crosshairCanvas.getScene().getWindow());
        if (file == null) {
            return;
        }
        Path path = file.toPath();
        if (!path.getFileName().toString().equalsIgnoreCase(fileName)) {
            setStatus("Bitte " + fileName + " auswählen.");
            return;
        }
        try {
            applyLoaded(Optional.of(repository.loadAndRemember(path)), true);
        } catch (Exception e) {
            setStatus("Fehler beim Laden: " + e.getMessage());
        }
    }
}
