package de.bsommerfeld.randomizer.ui.crosshair;

import de.bsommerfeld.randomizer.config.AppPreferences;
import de.bsommerfeld.randomizer.config.ConfigRepository;
import de.bsommerfeld.randomizer.config.ConfigSaver;
import de.bsommerfeld.randomizer.config.ConfigVerifier;
import de.bsommerfeld.randomizer.config.crosshair.Crosshair;
import de.bsommerfeld.randomizer.config.crosshair.CrosshairConvar;
import de.bsommerfeld.randomizer.config.crosshair.CrosshairRandomizer;
import de.bsommerfeld.randomizer.config.crosshair.CrosshairStandard;
import de.bsommerfeld.randomizer.exec.ExecApplier;
import de.bsommerfeld.randomizer.ui.FileChoosers;
import de.bsommerfeld.randomizer.vdf.VdfParseException;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Drives the "Fadenkreuz" tab: previews a crosshair over a switchable map background next to its
 * values. Editing works on an in-memory working copy restricted to the {@link CrosshairConvar}
 * catalogue - "Zufällig" fills it with random values, "Standard" restores the user's own standard
 * crosshair (its disk backup via {@link CrosshairStandard}), not CS2 defaults. "Speichern" writes
 * the working copy straight into the live .vcfg ({@link ConfigSaver} - only the catalogue's convars
 * change, everything else keeps its on-disk state) and additionally applies the values to a
 * running CS2 via {@link LiveApplier}. "Backup wiederherstellen" rolls the live file back
 * byte-exactly to the app-data backup and applies the restored values the same way.
 *
 * <p>The pieces are atomic collaborators, this class only coordinates them: {@link MapBackdrop}
 * (map cycling), {@link CrosshairPreview} (pixel drawing + value rows), {@link ExecKeySettings}
 * (exec-key editing + bind command) and {@link LiveApplier} (async transfer to a running CS2,
 * one-time prerequisite: {@code bind l "exec randomizer"} in the CS2 console).
 */
public final class CrosshairController {

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
    private final CrosshairRandomizer randomizer;
    private final ConfigSaver saver;
    private final ConfigVerifier verifier;

    private MapBackdrop backdrop;
    private CrosshairPreview preview;
    private LiveApplier liveApplier;

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
                               ExecApplier execApplier, AppPreferences preferences,
                               CrosshairRandomizer randomizer, ConfigSaver saver,
                               ConfigVerifier verifier) {
        this.repository = repository;
        this.standard = standard;
        this.execApplier = execApplier;
        this.preferences = preferences;
        this.randomizer = randomizer;
        this.saver = saver;
        this.verifier = verifier;
    }

    @FXML
    private void initialize() {
        preview = new CrosshairPreview(crosshairCanvas, valuesBox);
        backdrop = new MapBackdrop(backgroundImage, mapNameLabel, preview::screenFraction);
        backdrop.showFirst();
        liveApplier = new LiveApplier(execApplier);
        new ExecKeySettings(execKeyField, execKeyHint, bindCommandField, preferences, execApplier)
                .init();
        applyLoaded(repository.loadOnStartup(), false);
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
        preview.show(working);
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
     * Applies {@code working} to a running CS2 via {@link LiveApplier}; the async status is only
     * shown if no newer status exists by the time it arrives.
     */
    private void applyLive(String prefix) {
        int generation = statusGeneration;
        liveApplier.apply(working, prefix, status -> {
            if (generation == statusGeneration) {
                statusLabel.setText(status);
            }
        });
    }

    // ---- editing actions ---------------------------------------------------------------------

    @FXML
    private void onRandom() {
        working = randomizer.randomValues();
        preview.show(working);
        setStatus("Zufälliges Fadenkreuz - mit \"Speichern\" übernehmen, \"Standard\" setzt zurück.");
    }

    @FXML
    private void onReset() {
        working = new LinkedHashMap<>(standardValues);
        preview.show(working);
        setStatus("Standard-Fadenkreuz wiederhergestellt (noch nicht gespeichert).");
    }

    @FXML
    private void onSave() {
        if (livePath == null) {
            return;
        }
        try {
            saver.save(livePath, working);
            verifySaved();
            setStatus("Gespeichert: " + livePath + " - Übertrage an CS2…");
            applyLive("Gespeichert");
        } catch (IOException | VdfParseException e) {
            setStatus("Fehler beim Speichern: " + e.getMessage());
        }
    }

    /**
     * Re-reads the user settings just written and reports on the console when the crosshair values
     * did not actually end up in the file ({@link ConfigVerifier}).
     */
    private void verifySaved() {
        try {
            Map<String, String> missing = verifier.missingValues(livePath, working);
            if (!missing.isEmpty()) {
                System.err.println("Fadenkreuz nicht vollständig gesetzt - " + livePath
                        + " enthält folgende Werte nicht: " + missing);
            }
        } catch (IOException | VdfParseException e) {
            System.err.println("Fadenkreuz-Speicherung konnte nicht überprüft werden ("
                    + livePath + "): " + e);
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
