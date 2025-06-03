package de.bsommerfeld.randomizer.ui.view.controller;

import com.google.inject.Inject;
import de.bsommerfeld.randomizer.ui.view.View;
import de.bsommerfeld.randomizer.ui.view.ViewProvider;
import de.bsommerfeld.randomizer.ui.view.controller.settings.GeneralSettingsController;
import javafx.fxml.FXML;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.GridPane;

@View
public class SettingsViewController {

  private final ViewProvider viewProvider;

  @FXML private ToggleButton generalToggleButton;
  @FXML private GridPane contentPane;

  @Inject
  public SettingsViewController(ViewProvider viewProvider) {
    this.viewProvider = viewProvider;
  }

  @FXML
  private void initialize() {
    generalToggleButton.setSelected(true);
    contentPane
        .getChildren()
        .setAll(viewProvider.requestView(GeneralSettingsController.class).parent());
  }
}
