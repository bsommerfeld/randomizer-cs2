package de.bsommerfeld.randomizer.ui.view.controller.settings;


import de.bsommerfeld.randomizer.ui.view.View;
import java.util.function.BiConsumer;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import lombok.Setter;

@View
public class TitleDescriptionSettingsController {

    @FXML private VBox root;
    @FXML private TextArea textArea;
    @FXML private TextField textField;

    @Setter private BiConsumer<String, String> input;

    public TitleDescriptionSettingsController() {
        Platform.runLater(this::initialize);
    }

    @FXML
    void onApply(ActionEvent event) {
        if (input != null) {
            input.accept(textField.getText(), textArea.getText());
        }
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

    public void setTitle(String text) {
        textField.setText(text);
    }

    public void setDescription(String text) {
        textArea.setText(text);
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