package de.bsommerfeld.randomizer.ui;

import de.bsommerfeld.randomizer.config.CrosshairService;
import de.bsommerfeld.randomizer.config.CrosshairService.Crosshair;
import de.bsommerfeld.randomizer.config.CrosshairSettings;
import javafx.fxml.FXML;
import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

/**
 * Drives the "Fadenkreuz" tab: reads the crosshair convars from {@code cs2_user_convars.vcfg} and
 * previews the resulting crosshair, centered over a switchable map background, next to a list of the
 * raw values.
 */
public final class CrosshairController {

    private static final String IMAGE_DIR = "/de/bsommerfeld/randomizer/images/";
    private static final String[] MAP_FILES = {"mirage.png", "vertigo.png"};
    private static final String[] MAP_NAMES = {"Mirage", "Vertigo"};

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

    private final CrosshairService crosshairService;

    private int mapIndex;
    private CrosshairSettings settings;

    public CrosshairController(CrosshairService crosshairService) {
        this.crosshairService = crosshairService;
    }

    @FXML
    private void initialize() {
        showMap(0);
        show(crosshairService.loadOnStartup());
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
            statusLabel.setText("cs2_user_convars.vcfg nicht gefunden - bitte unten manuell wählen.");
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

    /**
     * Crops the background so it shows exactly the on-screen area the preview covers at the calibrated
     * resolution: the map image is treated as the whole screen, and only the centered fraction that
     * {@code canvasWidth} spans of a {@code 1920 * SCALE}-wide screen is shown — matching the crosshair
     * size so the in-game ratio is preserved.
     */
    private void applyViewport() {
        Image image = backgroundImage.getImage();
        if (image == null || image.getWidth() <= 0) {
            return;
        }
        double fraction = Math.min(1.0, crosshairCanvas.getWidth() / (1920.0 * SCALE));
        double vpW = image.getWidth() * fraction;
        double vpH = image.getHeight() * fraction;
        backgroundImage.setViewport(new Rectangle2D(
                (image.getWidth() - vpW) / 2, (image.getHeight() - vpH) / 2, vpW, vpH));
    }

    private void showMap(int index) {
        mapIndex = (index % MAP_FILES.length + MAP_FILES.length) % MAP_FILES.length;
        var url = getClass().getResource(IMAGE_DIR + MAP_FILES[mapIndex]);
        backgroundImage.setImage(url == null ? null : new Image(url.toExternalForm()));
        mapNameLabel.setText(MAP_NAMES[mapIndex]);
        applyViewport();
    }

    // ---- actions -----------------------------------------------------------------------------

    @FXML
    private void onPrevMap() {
        showMap(mapIndex - 1);
    }

    @FXML
    private void onNextMap() {
        showMap(mapIndex + 1);
    }

    @FXML
    private void onReload() {
        show(crosshairService.redetect());
    }

    @FXML
    private void onChoose() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(CrosshairService.FILE_NAME + " auswählen");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(CrosshairService.FILE_NAME, CrosshairService.FILE_NAME));
        File file = chooser.showOpenDialog(crosshairCanvas.getScene().getWindow());
        if (file == null) {
            return;
        }
        Path path = file.toPath();
        if (!path.getFileName().toString().equalsIgnoreCase(CrosshairService.FILE_NAME)) {
            statusLabel.setText("Bitte " + CrosshairService.FILE_NAME + " auswählen.");
            return;
        }
        try {
            show(Optional.of(crosshairService.loadAndRemember(path)));
        } catch (Exception e) {
            statusLabel.setText("Fehler beim Laden: " + e.getMessage());
        }
    }
}
