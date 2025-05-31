package de.bsommerfeld.randomizer.ui.view;

import javafx.scene.Parent;

/**
 * A record that bundles a JavaFX parent node and its associated controller of type <T>.
 *
 * @param parent     the root node of the FXML scene graph
 * @param controller the controller responsible for handling the events and interactions of the FXML view
 * @param <T>        the type of the controller
 */
public record ViewWrapper<T>(Parent parent, T controller) {
}
