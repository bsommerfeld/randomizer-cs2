package de.bsommerfeld.randomizer.ui.crosshair;

import de.bsommerfeld.randomizer.config.ConfigRepository;
import de.bsommerfeld.randomizer.config.crosshair.Crosshair;
import de.bsommerfeld.randomizer.config.crosshair.CrosshairSettings;
import de.bsommerfeld.randomizer.ui.FileChoosers;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

/**
 * Drives the "Fadenkreuz" tab: reads the crosshair convars from {@code cs2_user_convars.vcfg} and
 * previews the resulting crosshair, centered over a switchable map background, next to a list of the
 * raw values. Map cycling lives in {@link MapBackdrop}, pixel drawing in {@link CrosshairRenderer}.
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

    private final ConfigRepository<Crosshair> repository;

    private MapBackdrop backdrop;
    private CrosshairSettings settings;

    public CrosshairController(ConfigRepository<Crosshair> repository) {
        this.repository = repository;
    }

    @FXML
    private void initialize() {
        backdrop = new MapBackdrop(backgroundImage, mapNameLabel,
                () -> crosshairCanvas.getWidth() / (1920.0 * SCALE));
        backdrop.showFirst();
        show(repository.loadOnStartup());
    }

    // ---- data --------------------------------------------------------------------------------

    private void show(Optional<Crosshair> loaded) {
        loaded.ifPresentOrElse(crosshair -> {
            settings = crosshair.settings();
            statusLabel.setText("Geladen: " + crosshair.source());
            populateValues(crosshair.convars());
            drawCrosshair();
        }, () -> {
            settings = null;
            statusLabel.setText(repository.source().fileName()
                    + " nicht gefunden - bitte unten manuell wählen.");
            valuesBox.getChildren().clear();
            drawCrosshair();
        });
    }

    private void populateValues(Map<String, String> convars) {
        valuesBox.getChildren().clear();
        if (convars.isEmpty()) {
            valuesBox.getChildren().add(new Label("Keine cl_crosshair-Werte gefunden."));
            return;
        }
        convars.forEach((key, value) -> {
            Label row = new Label(key + " = " + value);
            row.getStyleClass().add("crosshair-value");
            valuesBox.getChildren().add(row);
        });
    }

    // ---- preview -----------------------------------------------------------------------------

    private void drawCrosshair() {
        GraphicsContext g = crosshairCanvas.getGraphicsContext2D();
        g.clearRect(0, 0, crosshairCanvas.getWidth(), crosshairCanvas.getHeight());
        if (settings != null) {
            CrosshairRenderer.draw(g, crosshairCanvas.getWidth() / 2, crosshairCanvas.getHeight() / 2,
                    settings, SCALE);
        }
    }

    // ---- actions -----------------------------------------------------------------------------

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
        show(repository.redetect());
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
            statusLabel.setText("Bitte " + fileName + " auswählen.");
            return;
        }
        try {
            show(Optional.of(repository.loadAndRemember(path)));
        } catch (Exception e) {
            statusLabel.setText("Fehler beim Laden: " + e.getMessage());
        }
    }
}
