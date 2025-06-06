package de.bsommerfeld.randomizer.ui.view.controller.builder.filler;

import com.google.inject.Inject;
import de.bsommerfeld.randomizer.config.RandomizerConfig;
import de.bsommerfeld.randomizer.ui.view.View;
import de.bsommerfeld.randomizer.ui.view.ViewProvider;
import de.bsommerfeld.randomizer.ui.view.controller.SettingsViewController;
import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.layout.HBox;

@View
public class BuilderFillerViewController {

  private final ViewProvider viewProvider;
  private final RandomizerConfig randomizerConfig;
  @FXML private HBox configIndicatorHBox;

  @Inject
  public BuilderFillerViewController(ViewProvider viewProvider, RandomizerConfig randomizerConfig) {
    this.viewProvider = viewProvider;
    this.randomizerConfig = randomizerConfig;
  }

  @FXML
  private void initialize() {
    configIndicatorHBox
        .visibleProperty()
        .bind(
            Bindings.createBooleanBinding(
                () ->
                    randomizerConfig.getConfigPath() == null
                        || randomizerConfig.getConfigPath().isEmpty(),
                randomizerConfig.getConfigPathProperty()));
  }

  @FXML
  public void onHyperlink(ActionEvent event) {
    viewProvider.triggerViewChange(SettingsViewController.class);
  }
}
