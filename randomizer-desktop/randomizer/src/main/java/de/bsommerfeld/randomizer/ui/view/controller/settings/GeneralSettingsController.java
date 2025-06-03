package de.bsommerfeld.randomizer.ui.view.controller.settings;

import com.google.inject.Inject;
import de.bsommerfeld.randomizer.ui.view.View;
import de.bsommerfeld.randomizer.ui.view.component.MinMaxSlider;
import de.bsommerfeld.randomizer.ui.view.viewmodel.settings.GeneralSettingsViewModel;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@View
public class GeneralSettingsController {

  private static final String CONFIG_FILE_TITLE = "Choose CS2 Config";
  private static final String CONFIG_FILE_EXTENSION = "*.vcfg";
  private static final String CONFIG_FILE_DESCRIPTION = "VCFG";

  private final GeneralSettingsViewModel generalSettingsViewModel;

  @FXML private ToggleButton showIntroToggleButton;
  @FXML private ToggleButton cs2FocusNeededToggleButton;
  @FXML private Button syncConfigButton;
  @FXML private TextField configPathTextField;
  @FXML private MinMaxSlider minMaxSlider;
  @FXML private Label syncFailedIndicator;

  @Inject
  public GeneralSettingsController(GeneralSettingsViewModel generalSettingsViewModel) {
    this.generalSettingsViewModel = generalSettingsViewModel;
  }

  @FXML
  private void initialize() {
    generalSettingsViewModel.setupViewModel();
    setupSettingsOptions();
    setupIntervalSlider();
    syncStatus();
  }

  private void setupSettingsOptions() {
    configPathTextField
        .textProperty()
        .bindBidirectional(generalSettingsViewModel.getConfigPathProperty());
    configPathTextField.setText(generalSettingsViewModel.getConfigPath());
    Tooltip tooltip = new Tooltip("Reload Config");
    tooltip.getStyleClass().add("tooltip-user-options");
    syncConfigButton.setTooltip(tooltip);
    showIntroToggleButton
        .selectedProperty()
        .bindBidirectional(generalSettingsViewModel.getShowIntroProperty());
    cs2FocusNeededToggleButton
        .selectedProperty()
        .bindBidirectional(generalSettingsViewModel.getCs2FocusNeededProperty());
  }

  private void setupIntervalSlider() {
    Platform.runLater(
        () ->
            minMaxSlider.setMinMaxValue(
                generalSettingsViewModel.getMinIntervalProperty().get(),
                generalSettingsViewModel.getMaxIntervalProperty().get()));

    minMaxSlider
        .getMinProperty()
        .bindBidirectional(generalSettingsViewModel.getMinIntervalProperty());
    minMaxSlider
        .getMaxProperty()
        .bindBidirectional(generalSettingsViewModel.getMaxIntervalProperty());
  }

  @FXML
  private void onConfigSync(ActionEvent event) {
    generalSettingsViewModel
        .loadConfigs()
        .exceptionally(
            throwable -> {
              generalSettingsViewModel.setConfigPath("");
              return null;
            });
  }

  private void syncStatus() {
    BooleanBinding configPathExists =
        generalSettingsViewModel
            .getConfigPathProperty()
            .isNotNull()
            .and(generalSettingsViewModel.getConfigPathProperty().isNotEmpty());

    configPathExists.addListener(
        (obs, oldVal, newVal) -> {
          syncConfigButton
              .getStyleClass()
              .removeAll("sync-config-path-success", "sync-config-path-failed");
          syncConfigButton
              .getStyleClass()
              .add(newVal ? "sync-config-path-success" : "sync-config-path-failed");
        });

    syncFailedIndicator.visibleProperty().bind(configPathExists.not());

    Platform.runLater(
        () -> {
          boolean isSuccess = configPathExists.get();
          syncConfigButton
              .getStyleClass()
              .removeAll("sync-config-path-success", "sync-config-path-failed");
          syncConfigButton
              .getStyleClass()
              .add(isSuccess ? "sync-config-path-success" : "sync-config-path-failed");
        });
  }

  @FXML
  private void onConfigChoose(ActionEvent event) {
    String currentConfigPath = generalSettingsViewModel.getCurrentConfigPath();

    File selectedFile = showConfigFileChooser(currentConfigPath);

    if (selectedFile != null) {
      log.info("Selected config file: {}", selectedFile.getAbsolutePath());
      String normalizedPath = normalizeFilePath(selectedFile.getAbsolutePath());
      generalSettingsViewModel.setConfigPath(normalizedPath);
      generalSettingsViewModel.reloadKeyBinds();
    }
  }

  private File showConfigFileChooser(String currentPath) {
    FileChooser fileChooser = new FileChooser();
    configureFileChooser(fileChooser, currentPath);
    return fileChooser.showOpenDialog(syncConfigButton.getScene().getWindow());
  }

  private void configureFileChooser(FileChooser fileChooser, String currentPath) {
    Path path = Path.of(currentPath);

    if (!Files.exists(path)) {
      fileChooser.setInitialDirectory(Path.of(System.getProperty("user.home")).toFile());
    } else {
      fileChooser.setInitialDirectory(path.toFile().getParentFile());
      fileChooser.setInitialFileName(path.getFileName().toString());
    }

    fileChooser.setTitle(CONFIG_FILE_TITLE);
    fileChooser
        .getExtensionFilters()
        .add(new FileChooser.ExtensionFilter(CONFIG_FILE_DESCRIPTION, CONFIG_FILE_EXTENSION));
  }

  private String normalizeFilePath(String filePath) {
    return filePath.replace("\\", "/");
  }
}
