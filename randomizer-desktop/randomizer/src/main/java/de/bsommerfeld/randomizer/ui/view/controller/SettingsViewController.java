package de.bsommerfeld.randomizer.ui.view.controller;

import com.google.inject.Inject;
import de.bsommerfeld.randomizer.ui.view.View;
import de.bsommerfeld.randomizer.ui.view.ViewProvider;
import de.bsommerfeld.randomizer.ui.view.controller.settings.GeneralSettingsController;
import de.bsommerfeld.randomizer.ui.view.controller.settings.MachineLearningSettingsController;
import javafx.fxml.FXML;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.GridPane;
import javafx.event.ActionEvent;

@View
public class SettingsViewController {

  private final ViewProvider viewProvider;

  @FXML private ToggleButton generalToggleButton;
  @FXML private ToggleButton machineLearningToggleButton;
  @FXML private GridPane contentPane;

  @Inject
  public SettingsViewController(ViewProvider viewProvider) {
    this.viewProvider = viewProvider;
  }

  @FXML
  private void initialize() {
    generalToggleButton.setOnAction(this::onGeneralToggleButtonAction);
    machineLearningToggleButton.setOnAction(this::onMachineLearningToggleButtonAction);

    generalToggleButton.setSelected(true);
    loadGeneralSettingsView();
  }

  @FXML
  private void onGeneralToggleButtonAction(ActionEvent event) {
    if (generalToggleButton.isSelected()) {
      machineLearningToggleButton.setSelected(false);
      loadGeneralSettingsView();
    } else {
      if (!machineLearningToggleButton.isSelected()) {
        generalToggleButton.setSelected(true);
      }
    }
  }

  @FXML
  private void onMachineLearningToggleButtonAction(ActionEvent event) {
    if (machineLearningToggleButton.isSelected()) {
      generalToggleButton.setSelected(false);
      loadMachineLearningView();
    } else {
      if (!generalToggleButton.isSelected()) {
        machineLearningToggleButton.setSelected(true);
      }
    }
  }

  private void loadGeneralSettingsView() {
    contentPane
        .getChildren()
        .setAll(viewProvider.requestView(GeneralSettingsController.class).parent());
  }

  private void loadMachineLearningView() {
    contentPane
        .getChildren()
        .setAll(viewProvider.requestView(MachineLearningSettingsController.class).parent());
  }
}
