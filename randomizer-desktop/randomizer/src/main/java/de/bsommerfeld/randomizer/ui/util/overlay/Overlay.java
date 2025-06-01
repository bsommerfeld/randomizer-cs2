package de.bsommerfeld.randomizer.ui.util.overlay;

import javafx.animation.FadeTransition;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Popup;
import javafx.stage.Window;
import javafx.util.Duration;

public class Overlay {
  private final Node overlayContent;
  private final Parent targetContainer;
  private final StackPane overlayWrapper;
  private final Popup popup;
  private final BooleanProperty dissolved = new SimpleBooleanProperty(false);
  private ChangeListener<Boolean> visibilityListener;
  private ChangeListener<Boolean> managedListener;
  private ChangeListener<Bounds> boundsListener;

  private Overlay(
      Node overlayContent, Parent targetContainer, StackPane overlayWrapper, Popup popup) {
    this.overlayContent = overlayContent;
    this.targetContainer = targetContainer;
    this.overlayWrapper = overlayWrapper;
    this.popup = popup;

    setupAutoDissolve();
    setupPositionTracking();
  }

  /**
   * Erstellt ein Overlay über dem angegebenen Container
   *
   * @param overlayPane Der Inhalt des Overlays
   * @param overlayOn Der Container, über dem das Overlay angezeigt werden soll
   * @return Eine Overlay-Instanz zur Verwaltung
   */
  public static Overlay overlay(Node overlayPane, Parent overlayOn) {
    // Scene und Window finden
    Scene scene = overlayOn.getScene();
    if (scene == null) {
      throw new IllegalStateException("Der Zielcontainer muss in einer Scene sein");
    }

    Window window = scene.getWindow();
    if (window == null) {
      throw new IllegalStateException("Die Scene muss in einem Window sein");
    }

    // Wrapper für das Overlay erstellen
    StackPane overlayWrapper = new StackPane();
    overlayWrapper.setStyle(
        "-fx-background-color: rgba(0, 0, 0, 0.3);"); // Semi-transparenter Hintergrund
    overlayWrapper.getChildren().add(overlayPane);

    // Popup erstellen
    Popup popup = new Popup();
    popup.getContent().add(overlayWrapper);
    popup.setAutoHide(false); // Wir verwalten das Schließen selbst

    // Position und Größe berechnen
    Bounds bounds = overlayOn.localToScreen(overlayOn.getBoundsInLocal());
    overlayWrapper.setPrefWidth(bounds.getWidth());
    overlayWrapper.setPrefHeight(bounds.getHeight());

    // Popup anzeigen
    popup.show(window, bounds.getMinX(), bounds.getMinY());

    // Fade-in Animation
    overlayWrapper.setOpacity(0.0);
    FadeTransition fadeIn = new FadeTransition(Duration.millis(200), overlayWrapper);
    fadeIn.setFromValue(0.0);
    fadeIn.setToValue(1.0);
    fadeIn.play();

    return new Overlay(overlayPane, overlayOn, overlayWrapper, popup);
  }

  private void setupPositionTracking() {
    // Listener für Positionsänderungen des Zielcontainers
    boundsListener =
        (obs, oldBounds, newBounds) -> {
          if (!dissolved.get() && popup.isShowing()) {
            updatePosition();
          }
        };

    // Bounds-Änderungen verfolgen
    targetContainer.boundsInLocalProperty().addListener(boundsListener);

    // Auch Layout-Änderungen verfolgen
    targetContainer.layoutBoundsProperty().addListener(boundsListener);
  }

  private void updatePosition() {
    try {
      Bounds bounds = targetContainer.localToScreen(targetContainer.getBoundsInLocal());
      if (bounds != null) {
        overlayWrapper.setPrefWidth(bounds.getWidth());
        overlayWrapper.setPrefHeight(bounds.getHeight());

        // Position aktualisieren
        popup.setX(bounds.getMinX());
        popup.setY(bounds.getMinY());
      }
    } catch (Exception e) {
      // Falls der Container nicht mehr in der Scene ist
      dissolve();
    }
  }

  private void setupAutoDissolve() {
    // Listener für Visibility-Änderungen
    visibilityListener =
        (obs, oldVal, newVal) -> {
          if (!newVal) { // Wenn nicht mehr sichtbar
            dissolve();
          }
        };

    // Listener für Managed-Änderungen
    managedListener =
        (obs, oldVal, newVal) -> {
          if (!newVal) { // Wenn nicht mehr managed
            dissolve();
          }
        };

    // Listener registrieren
    overlayContent.visibleProperty().addListener(visibilityListener);
    overlayContent.managedProperty().addListener(managedListener);

    // Auch auf Zielcontainer-Änderungen reagieren
    targetContainer
        .visibleProperty()
        .addListener(
            (obs, oldVal, newVal) -> {
              if (!newVal) {
                dissolve();
              }
            });

    // Window-Close Event abfangen
    targetContainer
        .sceneProperty()
        .addListener(
            (obs, oldScene, newScene) -> {
              if (newScene == null) {
                dissolve();
              }
            });
  }

  /** Entfernt das Overlay mit einer Fade-out Animation */
  public void dissolve() {
    if (dissolved.get()) {
      return; // Bereits dissolved
    }

    dissolved.set(true);

    // Listener entfernen
    if (visibilityListener != null) {
      overlayContent.visibleProperty().removeListener(visibilityListener);
    }
    if (managedListener != null) {
      overlayContent.managedProperty().removeListener(managedListener);
    }
    if (boundsListener != null) {
      targetContainer.boundsInLocalProperty().removeListener(boundsListener);
      targetContainer.layoutBoundsProperty().removeListener(boundsListener);
    }

    // Fade-out Animation
    FadeTransition fadeOut = new FadeTransition(Duration.millis(200), overlayWrapper);
    fadeOut.setFromValue(1.0);
    fadeOut.setToValue(0.0);
    fadeOut.setOnFinished(
        e -> {
          // Popup schließen
          if (popup.isShowing()) {
            popup.hide();
          }
        });
    fadeOut.play();
  }

  /**
   * @return true wenn das Overlay bereits dissolved wurde
   */
  public boolean isDissolved() {
    return dissolved.get();
  }

  /**
   * @return Property für den dissolved-Status
   */
  public BooleanProperty dissolvedProperty() {
    return dissolved;
  }

  /**
   * @return Der Overlay-Inhalt
   */
  public Node getOverlayContent() {
    return overlayContent;
  }

  /**
   * @return Der Container, auf dem das Overlay liegt
   */
  public Parent getTargetContainer() {
    return targetContainer;
  }

  /**
   * @return Das Popup-Objekt (für erweiterte Konfiguration)
   */
  public Popup getPopup() {
    return popup;
  }
}
