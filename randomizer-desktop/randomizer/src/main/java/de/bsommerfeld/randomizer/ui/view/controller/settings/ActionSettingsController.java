package de.bsommerfeld.randomizer.ui.view.controller.settings;

import com.google.inject.Inject;
import de.bsommerfeld.model.action.Action;
import de.bsommerfeld.randomizer.ui.view.View;
import de.bsommerfeld.randomizer.ui.view.component.MinMaxSlider;
import de.bsommerfeld.randomizer.ui.view.viewmodel.settings.ActionSettingsViewModel;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

@View
public class ActionSettingsController {

  private final ActionSettingsViewModel actionSettingsViewModel;

  @FXML private VBox actionSettingsVBox;
  @FXML private Label actionInFocusLabel;
  @FXML private MinMaxSlider minMaxSlider;

  @Inject
  public ActionSettingsController(ActionSettingsViewModel actionSettingsViewModel) {
    this.actionSettingsViewModel = actionSettingsViewModel;
  }

  @FXML
  void onClear(ActionEvent event) {
    minMaxSlider.setMinMaxValue(0, 1);
  }

  @FXML
  private void initialize() {
    initializeMinMaxSlider();
    setupBindings();
  }

  private void initializeMinMaxSlider() {
    minMaxSlider.setTimeUnit(MinMaxSlider.TimeUnit.MILLISECONDS);
    minMaxSlider.setMinLowerValue(0);
    minMaxSlider.setMaxHigherValue(9999);
    minMaxSlider.showLabels(false);
    minMaxSlider
        .getMinProperty()
        .bindBidirectional(actionSettingsViewModel.getMinIntervalProperty());
    minMaxSlider
        .getMaxProperty()
        .bindBidirectional(actionSettingsViewModel.getMaxIntervalProperty());
  }

  private void setupBindings() {
    // this is important, since if there is no action to adjust,
    // we don't need this view
    actionSettingsVBox
        .visibleProperty()
        .bind(actionSettingsViewModel.getActionInFocusProperty().isNotNull());

    actionSettingsViewModel
        .getActionInFocusProperty()
        .addListener(
            (_, _, newValue) -> {
              if (newValue == null) return;
              actionInFocusLabel.setText(newValue.getName());

              /*
               * even if it seems nonsensly to set this manually, since we already bound it bidirectional
               * to each other, this is still needed, because otherwise the values are not correctly set whyever.
               */
              Platform.runLater(
                  () -> {
                    minMaxSlider.setMinMaxValue(
                        actionSettingsViewModel.getMinIntervalProperty().get(),
                        actionSettingsViewModel.getMaxIntervalProperty().get());
                  });
            });
  }

  public void bindOnVisibleProperty(Consumer<Boolean> consumer) {
    actionSettingsVBox.visibleProperty().addListener((_, _, newValue) -> consumer.accept(newValue));
  }

  public void onChange(Runnable callback) {
    minMaxSlider.getMinProperty().addListener((_, _, newNumber) -> callback.run());
    minMaxSlider.getMaxProperty().addListener((_, _, newNumber) -> callback.run());
  }

  public void setAction(Action action) {
    actionSettingsViewModel.getActionInFocusProperty().set(action);
  }
}
