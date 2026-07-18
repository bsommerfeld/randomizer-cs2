package de.bsommerfeld.randomizer.ui.crosshair;

import de.bsommerfeld.randomizer.config.crosshair.CrosshairSettings;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;

import java.util.Map;

/**
 * The crosshair preview: draws the current values as pixels onto the canvas
 * ({@link CrosshairRenderer}) and lists them as "key = value" rows next to it. Owns the resolution
 * calibration shared with the {@link MapBackdrop} viewport.
 */
final class CrosshairPreview {

    /**
     * Calibrated to the bundled 1440p screenshots: the crosshair is drawn at real 1440p pixels and
     * the background is shown 1:1 (crosshair pixels scale with vertical resolution, 1080p being 1.0).
     */
    private static final double SCALE = 1440.0 / 1080.0;

    private final Canvas canvas;
    private final Pane valuesBox;

    CrosshairPreview(Canvas canvas, Pane valuesBox) {
        this.canvas = canvas;
        this.valuesBox = valuesBox;
    }

    /** Renders {@code values}: the value rows and the crosshair drawing. */
    void show(Map<String, String> values) {
        populateValues(values);
        drawCrosshair(CrosshairSettings.fromConvars(values));
    }

    /** The centered fraction (0..1) of the full screen the canvas covers - for {@link MapBackdrop}. */
    double screenFraction() {
        return canvas.getWidth() / (1920.0 * SCALE);
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
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        CrosshairRenderer.draw(g, canvas.getWidth() / 2, canvas.getHeight() / 2, settings, SCALE);
    }
}
