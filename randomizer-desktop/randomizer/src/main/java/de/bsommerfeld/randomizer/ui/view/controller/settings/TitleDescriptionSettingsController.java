package de.bsommerfeld.randomizer.ui.view.controller.settings;

import de.bsommerfeld.randomizer.ui.view.View;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

@View
public class TitleDescriptionSettingsController {

  @FXML private VBox root;
  @FXML private TextArea textArea;
  @FXML private TextField textField;

  public TitleDescriptionSettingsController() {
    Platform.runLater(this::initialize);
  }

  private void initialize() {
    textField
        .textProperty()
        .addListener(
            (_, oldValue, newValue) -> {
              if (newValue.length() > 50) {
                textField.setText(oldValue);
              }
            });
  }

  public StringProperty titleProperty() {
    return textField.textProperty();
  }

  public StringProperty descriptionProperty() {
    return textArea.textProperty();
  }

  public ReadOnlyBooleanProperty visibleProperty() {
    return root.visibleProperty();
  }
}
