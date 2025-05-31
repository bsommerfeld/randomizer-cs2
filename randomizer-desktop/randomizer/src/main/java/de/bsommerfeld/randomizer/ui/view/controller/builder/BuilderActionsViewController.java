package de.bsommerfeld.randomizer.ui.view.controller.builder;

import com.google.inject.Inject;
import de.bsommerfeld.model.action.Action;
import de.bsommerfeld.model.config.keybind.KeyBindType;
import de.bsommerfeld.model.persistence.JsonUtil;
import de.bsommerfeld.randomizer.config.RandomizerConfig;
import de.bsommerfeld.randomizer.ui.view.View;
import de.bsommerfeld.randomizer.ui.view.viewmodel.builder.BuilderViewModel;
import java.util.List;
import javafx.beans.property.BooleanProperty;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.FlowPane;
import javafx.util.Duration;

@View
public class BuilderActionsViewController {

  private static final PseudoClass EMPTY_PSEUDO_CLASS = PseudoClass.getPseudoClass("empty");

  private final RandomizerConfig randomizerConfig;
  private final BuilderViewModel builderViewModel;
  private final JsonUtil jsonUtil;

  @FXML private ToggleButton movementToggleButton;
  @FXML private ToggleButton weaponToggleButton;
  @FXML private ToggleButton inventoryToggleButton;
  @FXML private ToggleButton miscToggleButton;

  @FXML private FlowPane actionsFlowPane;
  @FXML private TextField searchField;

  @Inject
  public BuilderActionsViewController(
      RandomizerConfig randomizerConfig, BuilderViewModel builderViewModel, JsonUtil jsonUtil) {
    this.randomizerConfig = randomizerConfig;
    this.builderViewModel = builderViewModel;
    this.jsonUtil = jsonUtil;
  }

  @FXML
  private void initialize() {
    setupFilterToggleButtons();
    setupSearchField();
    updateActionDisplay(); // Initial display
  }

  private void setupFilterToggleButtons() {
    // Bind toggle button selection to config and initial setup
    bindToggleButton(movementToggleButton, KeyBindType.MOVEMENT);
    bindToggleButton(weaponToggleButton, KeyBindType.WEAPON);
    bindToggleButton(inventoryToggleButton, KeyBindType.INVENTORY);
    bindToggleButton(miscToggleButton, KeyBindType.MISCELLANEOUS);
  }

  private void bindToggleButton(ToggleButton toggleButton, KeyBindType keyBindType) {
    String keyBindTypeName = keyBindType.name();
    BooleanProperty selectedProperty = toggleButton.selectedProperty();

    // Initial state from config
    selectedProperty.set(randomizerConfig.getBuilderFiltersActivated().contains(keyBindTypeName));

    // Two-way binding:  UI changes -> config, and config changes -> UI
    selectedProperty.addListener(
        (observable, oldValue, newValue) -> {
          if (newValue) {
            randomizerConfig.getBuilderFiltersActivated().add(keyBindTypeName);
          } else {
            randomizerConfig.getBuilderFiltersActivated().remove(keyBindTypeName);
          }
          randomizerConfig.save();
          updateActionDisplay();
        });
  }

  private void setupSearchField() {
    setupSearchFieldPseudoClass();
    searchField
        .textProperty()
        .addListener((_, _, newValue) -> filterActionsBySearch(newValue.toLowerCase()));
  }

  private void setupSearchFieldPseudoClass() {
    searchField.pseudoClassStateChanged(EMPTY_PSEUDO_CLASS, searchField.getText().isEmpty());
    searchField
        .textProperty()
        .isEmpty()
        .addListener(
            (_, _, isNowEmpty) ->
                searchField.pseudoClassStateChanged(EMPTY_PSEUDO_CLASS, isNowEmpty));
  }

  private void filterActionsBySearch(String searchText) {
    actionsFlowPane.getChildren().clear();

    if (searchText.isEmpty()) {
      updateActionDisplay(); // Show all actions when search is empty
      return;
    }

    builderViewModel.getActionToTypeMap().values().stream()
        .flatMap(List::stream)
        .filter(action -> action.getName().toLowerCase().contains(searchText))
        .forEach(this::addActionLabel);
  }

  private void updateActionDisplay() {
    actionsFlowPane.getChildren().clear();

    List<KeyBindType> activeFilters =
        randomizerConfig.getBuilderFiltersActivated().stream().map(KeyBindType::valueOf).toList();

    builderViewModel.getActionToTypeMap().entrySet().stream()
        .filter(entry -> activeFilters.contains(entry.getKey()))
        .flatMap(entry -> entry.getValue().stream())
        .forEach(this::addActionLabel);
  }

  private void addActionLabel(Action action) {
    Label actionLabel = new Label(action.getName());
    actionLabel.getStyleClass().add("builder-actions-title");
    Tooltip tooltip = new Tooltip(action.getActionKey().getKey().toUpperCase());
    tooltip.setShowDelay(Duration.ZERO);
    actionLabel.setTooltip(tooltip);

    setupDragAndDrop(actionLabel, action);
    actionsFlowPane.getChildren().add(actionLabel);
  }

  private void setupDragAndDrop(Label label, Action action) {
    label.setCursor(Cursor.HAND);
    label.setOnDragDetected(
        event -> {
          Dragboard dragboard = label.startDragAndDrop(TransferMode.MOVE);
          dragboard.setDragView(label.snapshot(null, null), event.getX(), event.getY());

          ClipboardContent content = new ClipboardContent();
          content.putString(jsonUtil.serialize(action)); // Serialize the Action object
          dragboard.setContent(content);

          event.consume();
        });
  }
}
