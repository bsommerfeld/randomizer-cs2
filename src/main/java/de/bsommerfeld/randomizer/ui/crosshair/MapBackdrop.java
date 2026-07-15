package de.bsommerfeld.randomizer.ui.crosshair;

import javafx.geometry.Rectangle2D;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.util.function.DoubleSupplier;

/**
 * The switchable map background behind the crosshair preview: cycles through the bundled map
 * screenshots and crops each one to the screen fraction the preview covers, so the crosshair keeps
 * its in-game proportions.
 */
final class MapBackdrop {

    private static final String IMAGE_DIR = "/de/bsommerfeld/randomizer/images/";
    private static final String[] MAP_FILES = {"mirage.png", "vertigo.png"};
    private static final String[] MAP_NAMES = {"Mirage", "Vertigo"};

    private final ImageView imageView;
    private final Label nameLabel;
    private final DoubleSupplier screenFraction;

    private int index;

    /**
     * @param screenFraction the centered fraction (0..1) of the full screen the preview covers -
     *                       the backdrop shows exactly that part of the map image
     */
    MapBackdrop(ImageView imageView, Label nameLabel, DoubleSupplier screenFraction) {
        this.imageView = imageView;
        this.nameLabel = nameLabel;
        this.screenFraction = screenFraction;
    }

    void showFirst() {
        show(0);
    }

    void previous() {
        show(index - 1);
    }

    void next() {
        show(index + 1);
    }

    private void show(int newIndex) {
        index = (newIndex % MAP_FILES.length + MAP_FILES.length) % MAP_FILES.length;
        var url = getClass().getResource(IMAGE_DIR + MAP_FILES[index]);
        imageView.setImage(url == null ? null : new Image(url.toExternalForm()));
        nameLabel.setText(MAP_NAMES[index]);
        applyViewport();
    }

    /**
     * Crops the background so it shows exactly the on-screen area the preview covers: the map image
     * is treated as the whole screen, and only the centered {@code screenFraction} of it is shown.
     */
    private void applyViewport() {
        Image image = imageView.getImage();
        if (image == null || image.getWidth() <= 0) {
            return;
        }
        double fraction = Math.min(1.0, screenFraction.getAsDouble());
        double vpW = image.getWidth() * fraction;
        double vpH = image.getHeight() * fraction;
        imageView.setViewport(new Rectangle2D(
                (image.getWidth() - vpW) / 2, (image.getHeight() - vpH) / 2, vpW, vpH));
    }
}
