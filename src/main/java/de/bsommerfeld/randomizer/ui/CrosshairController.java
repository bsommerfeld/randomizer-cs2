package de.bsommerfeld.randomizer.ui;

import de.bsommerfeld.randomizer.config.CrosshairService;
import de.bsommerfeld.randomizer.config.CrosshairService.Crosshair;
import de.bsommerfeld.randomizer.config.CrosshairSettings;
import javafx.fxml.FXML;
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
 * previews the resulting crosshair, centered over one of four switchable map backgrounds, next to a
 * list of the raw values.
 */
public final class CrosshairController {

    private static final String IMAGE_DIR = "/de/bsommerfeld/randomizer/images/";
    private static final String[] MAP_FILES = {"dust2.png", "mirage.png", "ancient.png", "vertigo.png"};
    private static final String[] MAP_NAMES = {"Dust II", "Mirage", "Ancient", "Vertigo"};

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
            statusLabel.setText("cs2_user_convars.vcfg nicht gefunden — bitte unten manuell wählen.");
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
            CrosshairRenderer.draw(g, crosshairCanvas.getWidth() / 2, crosshairCanvas.getHeight() / 2, settings);
        }
    }

    private void showMap(int index) {
        mapIndex = (index % MAP_FILES.length + MAP_FILES.length) % MAP_FILES.length;
        backgroundImage.setImage(new Image(getClass().getResource(IMAGE_DIR + MAP_FILES[mapIndex]).toExternalForm()));
        mapNameLabel.setText(MAP_NAMES[mapIndex]);
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
