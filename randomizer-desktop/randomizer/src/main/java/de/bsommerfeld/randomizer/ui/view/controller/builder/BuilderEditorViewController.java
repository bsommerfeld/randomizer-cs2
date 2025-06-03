package de.bsommerfeld.randomizer.ui.view.controller.builder;

import com.google.inject.Inject;
import de.bsommerfeld.model.action.Action;
import de.bsommerfeld.model.action.sequence.ActionSequence;
import de.bsommerfeld.model.persistence.JsonUtil;
import de.bsommerfeld.randomizer.ui.RandomizerApplication;
import de.bsommerfeld.randomizer.ui.view.View;
import de.bsommerfeld.randomizer.ui.view.ViewProvider;
import de.bsommerfeld.randomizer.ui.view.ViewWrapper;
import de.bsommerfeld.randomizer.ui.view.controller.settings.ActionSettingsController;
import de.bsommerfeld.randomizer.ui.view.controller.settings.TitleDescriptionSettingsController;
import de.bsommerfeld.randomizer.ui.view.viewmodel.builder.BuilderViewModel;
import java.util.List;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.VBox;

@View
public class BuilderEditorViewController {

  private static final String ACTION_NAME_STYLING = "logbook-sequence-actions-name";
  private static final String START_ACTION_NAME_STYLING = "logbook-sequence-actions-name-start";
  private static final String MIDDLE_ACTION_NAME_STYLING = "logbook-sequence-actions-name-middle";
  private static final String END_ACTION_NAME_STYLING = "logbook-sequence-actions-name-end";
  private static final String START_ACTIVE_ACTION_NAME_STYLING =
      "logbook-sequence-actions-name-start-active";
  private static final String MIDDLE_ACTIVE_ACTION_NAME_STYLING =
      "logbook-sequence-actions-name-middle-active";
  private static final String END_ACTIVE_ACTION_NAME_STYLING =
      "logbook-sequence-actions-name-end-active";

  private final ObjectProperty<Label> labelInFocusProperty = new SimpleObjectProperty<>();
  private final Separator dropIndicator = new Separator();

  private final ViewProvider viewProvider;
  private final BuilderViewModel builderViewModel;
  private final JsonUtil jsonUtil;

  @FXML private VBox builderActionsPlaceholder;
  @FXML private VBox settingsHolder;
  @FXML private VBox actionSettingsHolder;

  @FXML private Label sequenceNameLabel;
  @FXML private Label sequenceDescriptionLabel;

  @FXML private Button randomizeButton;
  @FXML private Button saveSequenceButton;
  @FXML private Button actionsClearButton;

  @FXML private VBox builderVBox;

  private ActionSettingsController actionSettingsController;

  @Inject
  public BuilderEditorViewController(
      ViewProvider viewProvider, BuilderViewModel builderViewModel, JsonUtil jsonUtil) {
    this.viewProvider = viewProvider;
    this.builderViewModel = builderViewModel;
    this.jsonUtil = jsonUtil;
  }

  @FXML
  void onRandomize(ActionEvent event) {
    builderViewModel.addRandomActions(10);
  }

  @FXML
  void onActionsClear(ActionEvent event) {
    builderViewModel.setActions(List.of());
  }

  @FXML
  void onSaveSequence(ActionEvent event) {
    BuilderViewController controller =
        viewProvider.requestView(BuilderViewController.class).controller();

    if (!isWindowsConformFileName(builderViewModel.getSequenceNameProperty().get())) {
      showInvalidSequenceNameAlert();
      return;
    }

    if (doesAnotherActionSequenceWithThisNameExist()) {
      showSequenceWithNameAlreadyExistsAlert(controller);
      return;
    }

    builderViewModel.saveActionSequence();
    controller.fillActionSequences();
  }

  private void showSequenceWithNameAlreadyExistsAlert(BuilderViewController controller) {
    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
    alert.setTitle("Sequence Already Exists");
    alert.setHeaderText("Duplicate Sequence Name");
    alert.setContentText("A sequence with this name already exists. Do you want to overwrite it?");

    alert
        .getDialogPane()
        .getStylesheets()
        .add(RandomizerApplication.class.getResource("alert-style.css").toExternalForm());

    alert.getDialogPane().getStyleClass().addAll("modern-alert", "warning");

    ((Button) alert.getDialogPane().lookupButton(ButtonType.OK)).setText("Overwrite");
    ((Button) alert.getDialogPane().lookupButton(ButtonType.CANCEL)).setText("Cancel");
    alert.getDialogPane().lookupButton(ButtonType.CANCEL).getStyleClass().add("cancel-button");

    alert
        .showAndWait()
        .filter(response -> response == ButtonType.OK)
        .ifPresent(
            response -> {
              builderViewModel.saveActionSequence();
              controller.fillActionSequences();
            });
  }

  private void showInvalidSequenceNameAlert() {
    Alert alert = new Alert(Alert.AlertType.ERROR);
    alert.setTitle("Invalid Sequence Name");
    alert.setHeaderText("Name Contains Invalid Characters");
    alert.setContentText(
        "Please use only letters, numbers, spaces, hyphens, and periods in the sequence name.");

    alert
        .getDialogPane()
        .getStylesheets()
        .add(RandomizerApplication.class.getResource("alert-style.css").toExternalForm());

    alert.getDialogPane().getStyleClass().addAll("modern-alert", "error");

    ((Button) alert.getDialogPane().lookupButton(ButtonType.OK)).setText("Got it");

    alert.showAndWait();
  }

  private boolean isWindowsConformFileName(String name) {
    return name.matches("[a-zA-Z0-9_\\-\\. ]+");
  }

  private void setupBindings() {
    builderViewModel
        .getCurrentActionSequenceProperty()
        .addListener(
            (_, _, newSequence) -> {
              if (newSequence == null) return;
              ViewWrapper<TitleDescriptionSettingsController> tdsViewWrapper =
                  viewProvider.requestView(TitleDescriptionSettingsController.class);

              settingsHolder.getChildren().setAll(tdsViewWrapper.parent());

              TitleDescriptionSettingsController controller = tdsViewWrapper.controller();
              controller.setTitle(builderViewModel.getSequenceNameProperty().get());
              controller.setDescription(builderViewModel.getSequenceDescriptionProperty().get());

              // TODO: 02.03.2025 - Do this via bindings

              fillBuilderWithActionsOfSequence(newSequence);
              actionSettingsHolder.getChildren().clear();
            });

    builderViewModel
        .getCurrentActionsProperty()
        .addListener((ListChangeListener<Action>) _ -> updateBuilderVBox());

    builderVBox
        .disableProperty()
        .bind(builderViewModel.getCurrentActionSequenceProperty().isNull());

    randomizeButton
        .disableProperty()
        .bind(builderViewModel.getCurrentActionSequenceProperty().isNull());

    actionsClearButton
        .disableProperty()
        .bind(builderViewModel.getCurrentActionSequenceProperty().isNull());

    saveSequenceButton
        .disableProperty()
        .bind(builderViewModel.getCurrentActionSequenceProperty().isNull());

    sequenceDescriptionLabel.textProperty().bind(builderViewModel.getSequenceDescriptionProperty());
    sequenceNameLabel.textProperty().bind(builderViewModel.getSequenceNameProperty());

    labelInFocusProperty.addListener(
        (_, oldLabel, newLabel) -> {
          if (oldLabel != null) setPositionalStyling(oldLabel, false);
          if (newLabel != null) setPositionalStyling(newLabel, true);
        });
  }

  private void initActionSettings() {
    actionSettingsController =
        viewProvider.requestView(ActionSettingsController.class).controller();
    actionSettingsController.bindOnVisibleProperty(
        visible -> {
          if (visible)
            actionSettingsHolder
                .getChildren()
                .setAll(viewProvider.requestView(ActionSettingsController.class).parent());
          else {
            labelInFocusProperty.set(null);
            actionSettingsHolder.getChildren().clear();
          }
        });
  }

  private void setPositionalStyling(Label label, boolean active) {
    int index = builderVBox.getChildren().indexOf(label);

    if (index == 0) {
      label.getStyleClass().clear();
      label.getStyleClass().add(ACTION_NAME_STYLING);
      label
          .getStyleClass()
          .add(active ? START_ACTIVE_ACTION_NAME_STYLING : START_ACTION_NAME_STYLING);
      return;
    }

    if (index == builderVBox.getChildren().size() - 1) {
      label.getStyleClass().clear();
      label.getStyleClass().add(ACTION_NAME_STYLING);
      label.getStyleClass().add(active ? END_ACTIVE_ACTION_NAME_STYLING : END_ACTION_NAME_STYLING);
      return;
    }

    label.getStyleClass().clear();
    label.getStyleClass().add(ACTION_NAME_STYLING);
    label
        .getStyleClass()
        .add(active ? MIDDLE_ACTIVE_ACTION_NAME_STYLING : MIDDLE_ACTION_NAME_STYLING);
  }

  private boolean doesAnotherActionSequenceWithThisNameExist() {
    return builderViewModel.getActionSequences().stream()
        .filter(
            actionSequence ->
                !actionSequence.equals(builderViewModel.getCurrentActionSequenceProperty().get()))
        .anyMatch(
            actionSequence ->
                actionSequence
                    .getName()
                    .equalsIgnoreCase(builderViewModel.getSequenceNameProperty().get()));
  }

  @FXML
  private void initialize() {
    loadActionsView();
    initActionSettings();
    initTitleAndDescriptionSettings();
    initDropIndicator();
    setupBindings();
    setupDrop(builderVBox);
    initialFill();
  }

  private void initialFill() {
    fillBuilderWithActionsOfSequence(builderViewModel.getCurrentActionSequenceProperty().get());
    updateBuilderVBox();
  }

  private void setupDrop(VBox target) {
    target.setOnDragOver(
        dragEvent -> {
          if (dragEvent.getGestureSource() != target && dragEvent.getDragboard().hasString()) {
            dragEvent.acceptTransferModes(TransferMode.MOVE);
            // Hier muss der dropIndicator am Ende hinzugefügt werden, falls über die VBox gedragged
            // wird, und nicht über die labels
            if (!builderVBox.getChildren().contains(dropIndicator)) {
              builderVBox.getChildren().add(dropIndicator);
            }
          }
          dragEvent.consume();
        });

    target.setOnDragDropped(
        dragEvent -> {
          Dragboard dragboard = dragEvent.getDragboard();
          boolean success = false;

          if (dragboard.hasString()) {
            String serializedAction = dragboard.getString();
            Action droppedAction = jsonUtil.deserializeAction(serializedAction);

            // Stelle sicher, dass hier keine Exception geworfen wird, falls aus irgend einem Grund
            // kein Indikator gesetzt wurde
            if (builderVBox.getChildren().contains(dropIndicator)) {
              int index = builderVBox.getChildren().indexOf(dropIndicator);
              builderVBox.getChildren().remove(dropIndicator);
              builderViewModel.addActionAt(droppedAction, index);
            } else {
              // Wenn kein Indikator gesetzt wurde, am Ende einfügen
              builderViewModel.addAction(droppedAction);
            }
            success = true;
          }

          dragEvent.setDropCompleted(success);
          dragEvent.consume();
        });

    target.setOnDragExited(
        dragEvent -> {
          // Nur entfernen, wenn vorhanden
          if (builderVBox.getChildren().contains(dropIndicator)) {
            builderVBox.getChildren().remove(dropIndicator);
          }
          dragEvent.consume();
        });
  }

  private void initDropIndicator() {
    dropIndicator.getStyleClass().add("builder-separator");
  }

  private void initTitleAndDescriptionSettings() {
    ViewWrapper<TitleDescriptionSettingsController> tdsViewWrapper =
        viewProvider.requestView(TitleDescriptionSettingsController.class);

    settingsHolder.getChildren().setAll(tdsViewWrapper.parent());

    TitleDescriptionSettingsController controller = tdsViewWrapper.controller();
    controller.setTitle(builderViewModel.getSequenceNameProperty().get());
    controller.setDescription(builderViewModel.getSequenceDescriptionProperty().get());

    controller.setInput(
        (inputTitle, inputDescription) -> {
          if (inputTitle != null && !inputTitle.isBlank()) {
            builderViewModel.getSequenceNameProperty().set(inputTitle);
          }
          if (inputDescription != null && !inputDescription.isBlank()) {
            builderViewModel.getSequenceDescriptionProperty().set(inputDescription);
          }
        });
  }

  private void loadActionsView() {
    Parent parent = viewProvider.requestView(BuilderActionsViewController.class).parent();
    builderActionsPlaceholder.getChildren().add(parent);
  }

  private void fillBuilderWithActionsOfSequence(ActionSequence sequence) {
    if (sequence == null) return;
    List<Action> actions = builderViewModel.getActionsOfSequence(sequence);
    builderViewModel.setActions(actions);
  }

  private void updateBuilderVBox() {
    labelInFocusProperty.set(null);
    actionSettingsController.setAction(null);
    builderVBox.getChildren().clear();
    builderViewModel
        .getCurrentActionsProperty()
        .forEach(
            action -> {
              if (action == null) return; // in case an action is broken
              Label actionLabel = new Label(action.getName());
              actionLabel.setOnMouseClicked(
                  _ -> {
                    actionSettingsController.setAction(action);
                    labelInFocusProperty.set(actionLabel);
                  });
              setupDragAlreadyDropped(actionLabel, action); // setup special drag within listview
              builderVBox.getChildren().add(actionLabel);
            });

    builderVBox.getChildren().stream()
        .map(Label.class::cast)
        .forEach(node -> setPositionalStyling(node, false));
  }

  private void setupDragAlreadyDropped(Label label, Action action) {
    label.setCursor(Cursor.HAND);
    label.setOnDragDetected(
        dragEvent -> {
          Dragboard dragboard = label.startDragAndDrop(TransferMode.MOVE);
          dragboard.setDragView(label.snapshot(null, null), dragEvent.getX(), dragEvent.getY());

          ClipboardContent content = new ClipboardContent();
          String serializedAction = jsonUtil.serialize(action);
          builderViewModel.removeAction(
              action); // Entferne die Action *vor* dem Hinzufügen zum Dragboard
          content.putString(serializedAction);

          dragboard.setContent(content);
          dragEvent.consume();
        });

    label.setOnDragOver(
        dragEvent -> {
          if (dragEvent.getGestureSource() != label && dragEvent.getDragboard().hasString()) {
            dragEvent.acceptTransferModes(TransferMode.MOVE);

            int labelIndex = builderVBox.getChildren().indexOf(label);
            double mouseY = dragEvent.getY();
            double labelHeight = label.getHeight();

            // Vereinfachte Hysterese: Nur prüfen, ob Maus im oberen oder unteren Drittel des Labels
            // ist.
            if (mouseY < labelHeight / 3.0) {
              labelIndex = Math.max(0, labelIndex - 1); // Einfügen vor dem Label
            } else if (mouseY > labelHeight * 2.0 / 3.0) {
              labelIndex++; // Einfügen nach dem Label
            }
            // DropIndicator hinzufügen, falls nicht vorhanden.
            if (!builderVBox.getChildren().contains(dropIndicator)) {
              builderVBox.getChildren().add(labelIndex, dropIndicator);
            }
            // DropIndicator Position aktualisieren, wenn nötig.
            else if (builderVBox.getChildren().indexOf(dropIndicator) != labelIndex) {
              builderVBox.getChildren().remove(dropIndicator);
              builderVBox.getChildren().add(labelIndex, dropIndicator);
            }
          }
          dragEvent.consume();
        });

    label.setOnDragDropped(
        dragEvent -> {
          Dragboard dragboard = dragEvent.getDragboard();
          boolean success = false;

          if (dragboard.hasString()) {
            String serializedAction = dragboard.getString();
            Action droppedAction = jsonUtil.deserializeAction(serializedAction);

            int dropIndicatorIndex = builderVBox.getChildren().indexOf(dropIndicator);

            if (dropIndicatorIndex != -1) {
              builderViewModel.addActionAt(droppedAction, dropIndicatorIndex);
              success = true;
            }
          }

          dragEvent.setDropCompleted(success);
          builderVBox.getChildren().remove(dropIndicator); // Immer entfernen
          dragEvent.consume();
        });

    // Kein DragEntered mehr nötig
  }
}
