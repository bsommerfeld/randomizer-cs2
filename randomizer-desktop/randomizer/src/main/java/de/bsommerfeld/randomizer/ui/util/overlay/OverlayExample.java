package de.bsommerfeld.randomizer.ui.util.overlay;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class OverlayExample extends Application {

  public static void main(String[] args) {
    launch(args);
  }

  @Override
  public void start(Stage primaryStage) {
    // Hauptcontainer
    VBox mainContainer = new VBox(20);
    mainContainer.setStyle("-fx-padding: 20; -fx-alignment: center;");

    // Settings Button
    Button settingsButton = new Button("Settings öffnen");

    // Andere UI-Elemente
    Label mainLabel = new Label("Hauptmenü");

    mainContainer.getChildren().addAll(mainLabel, settingsButton);

    // Settings Button Event
    settingsButton.setOnAction(
        e -> {
          // Settings Menü erstellen
          VBox settingsMenu = createSettingsMenu();

          // Overlay erstellen
          Overlay overlay = Overlay.overlay(settingsMenu, mainContainer);

          // Optional: Overlay-Status überwachen
          overlay
              .dissolvedProperty()
              .addListener(
                  (obs, oldVal, newVal) -> {
                    if (newVal) {
                      System.out.println("Overlay wurde geschlossen");
                    }
                  });
        });

    Scene scene = new Scene(mainContainer, 400, 300);
    primaryStage.setTitle("Overlay Beispiel");
    primaryStage.setScene(scene);
    primaryStage.show();
  }

  private VBox createSettingsMenu() {
    VBox settingsMenu = new VBox(10);
    settingsMenu.setStyle(
        "-fx-background-color: white; -fx-border-color: gray; "
            + "-fx-border-width: 1; -fx-padding: 20; -fx-max-width: 300; "
            + "-fx-max-height: 200;");

    Label titleLabel = new Label("Settings");
    titleLabel.setStyle("-fx-font-size: 16; -fx-font-weight: bold;");

    Button closeButton = new Button("X");
    closeButton.setStyle("-fx-background-color: red; -fx-text-fill: white;");

    // Close Button Event - löst automatisch das Overlay auf
    closeButton.setOnAction(
        e -> {
          settingsMenu.setVisible(false); // Löst automatisch dissolve() aus
        });

    // Weitere Settings-Optionen
    Button option1 = new Button("Option 1");
    Button option2 = new Button("Option 2");

    settingsMenu.getChildren().addAll(titleLabel, closeButton, option1, option2);

    return settingsMenu;
  }
}
