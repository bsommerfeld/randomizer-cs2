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
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

@View
public class BuilderEditorViewController {

  private static final String ACTION_NAME_STYLING = "logbook-sequence-actions-name";
  private static final String START_ACTION_ICON_STYLING = "logbook-sequence-actions-icon-start";
  private static final String ACTION_ICON_STYLING = "logbook-sequence-actions-icon-start";
  private static final String MIDDLE_ACTION_ICON_STYLING = "logbook-sequence-actions-icon-middle";
  private static final String END_ACTION_ICON_STYLING = "logbook-sequence-actions-icon-end";
  private static final String START_ACTIVE_ACTION_ICON_STYLING =
      "logbook-sequence-actions-icon-start-active";
  private static final String MIDDLE_ACTIVE_ACTION_ICON_STYLING =
      "logbook-sequence-actions-icon-middle-active";
  private static final String END_ACTIVE_ACTION_ICON_STYLING =
      "logbook-sequence-actions-icon-end-active";

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

              controller
                  .titleProperty()
                  .bindBidirectional(builderViewModel.getSequenceNameProperty());
              controller
                  .descriptionProperty()
                  .bindBidirectional(builderViewModel.getSequenceDescriptionProperty());

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
          if (oldLabel != null) {
            HBox oldHBox = (HBox) oldLabel.getParent();
            ImageView oldImageView = (ImageView) oldHBox.getChildren().get(0);
            setPositionalStyling(oldImageView, false);
          }
          if (newLabel != null) {
            HBox newHBox = (HBox) newLabel.getParent();
            ImageView newImageView = (ImageView) newHBox.getChildren().get(0);
            setPositionalStyling(newImageView, true);
          }
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

  private void setPositionalStyling(ImageView imageView, boolean active) {
    HBox parentHBox = (HBox) imageView.getParent();
    int index = builderVBox.getChildren().indexOf(parentHBox);

    if (index == 0) {
      imageView.getStyleClass().clear();
      imageView.getStyleClass().add(ACTION_ICON_STYLING);
      imageView
          .getStyleClass()
          .add(active ? START_ACTIVE_ACTION_ICON_STYLING : START_ACTION_ICON_STYLING);
      return;
    }

    if (index == builderVBox.getChildren().size() - 1) {
      imageView.getStyleClass().clear();
      imageView.getStyleClass().add(ACTION_ICON_STYLING);
      imageView
          .getStyleClass()
          .add(active ? END_ACTIVE_ACTION_ICON_STYLING : END_ACTION_ICON_STYLING);
      return;
    }

    imageView.getStyleClass().clear();
    imageView.getStyleClass().add(ACTION_ICON_STYLING);
    imageView
        .getStyleClass()
        .add(active ? MIDDLE_ACTIVE_ACTION_ICON_STYLING : MIDDLE_ACTION_ICON_STYLING);
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

    controller.titleProperty().bindBidirectional(builderViewModel.getSequenceNameProperty());
    controller
        .descriptionProperty()
        .bindBidirectional(builderViewModel.getSequenceDescriptionProperty());

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

              HBox actionContainer = new HBox();
              actionContainer.getStyleClass().add("logbook-sequence-actions-container");

              ImageView actionIcon = new ImageView();
              actionIcon.getStyleClass().add(ACTION_ICON_STYLING);

              Label actionLabel = new Label(action.getName());
              actionLabel.getStyleClass().add(ACTION_NAME_STYLING);

              actionContainer.getChildren().addAll(actionIcon, actionLabel);

              actionContainer.setOnMouseClicked(
                  _ -> {
                    actionSettingsController.setAction(action);
                    labelInFocusProperty.set(actionLabel);
                  });

              setupDragAlreadyDropped(actionContainer, action);
              builderVBox.getChildren().add(actionContainer);
            });

    builderVBox.getChildren().stream()
        .map(HBox.class::cast)
        .forEach(
            hbox -> {
              ImageView imageView = (ImageView) hbox.getChildren().get(0);
              setPositionalStyling(imageView, false);
            });
  }

  private void setupDragAlreadyDropped(HBox container, Action action) {
    container.setCursor(Cursor.HAND);
    container.setOnDragDetected(
        dragEvent -> {
          Dragboard dragboard = container.startDragAndDrop(TransferMode.MOVE);
          dragboard.setDragView(container.snapshot(null, null), dragEvent.getX(), dragEvent.getY());

          ClipboardContent content = new ClipboardContent();
          String serializedAction = jsonUtil.serialize(action);
          builderViewModel.removeAction(
              action); // Entferne die Action *vor* dem Hinzufügen zum Dragboard
          content.putString(serializedAction);

          dragboard.setContent(content);
          dragEvent.consume();
        });

    container.setOnDragOver(
        dragEvent -> {
          if (dragEvent.getGestureSource() != container && dragEvent.getDragboard().hasString()) {
            dragEvent.acceptTransferModes(TransferMode.MOVE);

            int containerIndex = builderVBox.getChildren().indexOf(container);
            double mouseY = dragEvent.getY();
            double containerHeight = container.getHeight();

            // Vereinfachte Hysterese: Nur prüfen, ob Maus im oberen oder unteren Drittel des
            // Containers ist.
            if (mouseY < containerHeight / 3.0) {
              containerIndex = Math.max(0, containerIndex - 1); // Einfügen vor dem Container
            } else if (mouseY > containerHeight * 2.0 / 3.0) {
              containerIndex++; // Einfügen nach dem Container
            }
            // DropIndicator hinzufügen, falls nicht vorhanden.
            if (!builderVBox.getChildren().contains(dropIndicator)) {
              builderVBox.getChildren().add(containerIndex, dropIndicator);
            }
            // DropIndicator Position aktualisieren, wenn nötig.
            else if (builderVBox.getChildren().indexOf(dropIndicator) != containerIndex) {
              builderVBox.getChildren().remove(dropIndicator);
              builderVBox.getChildren().add(containerIndex, dropIndicator);
            }
          }
          dragEvent.consume();
        });

    container.setOnDragDropped(
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
  }
}
