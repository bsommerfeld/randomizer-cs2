package de.bsommerfeld.randomizer.ui.view.controller;

import com.google.inject.Inject;
import de.bsommerfeld.model.ApplicationState;
import de.bsommerfeld.model.action.sequence.ActionSequence;
import de.bsommerfeld.randomizer.ui.view.View;
import de.bsommerfeld.randomizer.ui.view.viewmodel.RandomizerViewModel;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

@View
public class RandomizerViewController {

  private static final String ACTION_ICON_STYLING = "logbook-sequence-actions-icon";
  private static final String START_ACTION_ICON_STYLING = "logbook-sequence-actions-icon-start";
  private static final String MIDDLE_ACTION_ICON_STYLING = "logbook-sequence-actions-icon-middle";
  private static final String END_ACTION_ICON_STYLING = "logbook-sequence-actions-icon-end";
  private static final String START_ACTIVE_ACTION_ICON_STYLING =
      "logbook-sequence-actions-icon-start-active";
  private static final String MIDDLE_ACTIVE_ACTION_ICON_STYLING =
      "logbook-sequence-actions-icon-middle-active";
  private static final String END_ACTIVE_ACTION_ICON_STYLING =
      "logbook-sequence-actions-icon-end-active";

  private final RandomizerViewModel randomizerViewModel;

  @FXML private Label sequenceNameLabel;
  @FXML private VBox actionsVBox;
  @FXML private VBox historyVBox;
  @FXML private ToggleButton randomizerToggleButton;
  @FXML private ImageView cs2FocusImage;

  @Inject
  public RandomizerViewController(RandomizerViewModel randomizerViewModel) {
    this.randomizerViewModel = randomizerViewModel;
  }

  @FXML
  void onToggle(ActionEvent event) {
    if (!randomizerToggleButton.isSelected()) {
      randomizerViewModel.setApplicationStateToStopped();
      clearCurrentSequenceView();
      randomizerToggleButton.setText("Start");
    } else {
      randomizerViewModel.setApplicationStateToRunning();
      randomizerToggleButton.setText("Stop");
    }
  }

  @FXML
  private void initialize() {
    setupBindings();
    setupListener();
    setupStateListener();
  }

  private void clearCurrentSequenceView() {
    actionsVBox.getChildren().clear();
    sequenceNameLabel.setText("");
  }

  private void setupBindings() {
    sequenceNameLabel.visibleProperty().bind(sequenceNameLabel.textProperty().isNotEmpty());
  }

  /** Creates the history container for the ActionSequence */
  private void createHistoryContainer(ActionSequence actionSequence) {
    HBox container = new HBox();
    container.getStyleClass().add("logbook-history-entry-container");

    Label actionSequenceNameLabel = new Label(actionSequence.getName());
    actionSequenceNameLabel.getStyleClass().add("logbook-history-entry-name");

    HBox centerBox = new HBox();
    HBox.setHgrow(centerBox, Priority.ALWAYS);
    centerBox.getStyleClass().add("logbook-history-entry-rightbox");
    Label actionSequenceActionCount = new Label(actionSequence.getActions().size() + " Actions");
    actionSequenceActionCount.getStyleClass().add("logbook-history-entry-action-count");

    HBox rightFiller = new HBox();
    HBox.setHgrow(rightFiller, Priority.ALWAYS);

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
    Label actionSequenceActionExecutedAt = new Label(LocalTime.now().format(formatter));
    actionSequenceActionExecutedAt.getStyleClass().add("logbook-history-entry-executed-at");
    centerBox
        .getChildren()
        .addAll(actionSequenceActionCount, rightFiller, actionSequenceActionExecutedAt);

    HBox leftFiller = new HBox();
    HBox.setHgrow(leftFiller, Priority.ALWAYS);

    container.getChildren().addAll(actionSequenceNameLabel, leftFiller, centerBox);
    historyVBox.getChildren().addFirst(container);
  }

  private void setupListener() {
    randomizerViewModel.onActionSequenceFinished(
        actionSequence ->
            Platform.runLater(
                () -> {
                  createHistoryContainer(actionSequence);
                  clearCurrentSequenceView();
                }));

    randomizerViewModel
        .getCurrentActionSequenceProperty()
        .addListener(
            (_, _, sequence) -> {
              if (sequence == null) {
                return;
              }

              Platform.runLater(
                  () -> {
                    sequenceNameLabel.setText(sequence.getName());
                    actionsVBox.getChildren().clear();
                    sequence
                        .getActions()
                        .forEach(
                            action -> {
                              if (action == null) return;

                              HBox sequenceAction = new HBox();
                              sequenceAction
                                  .getStyleClass()
                                  .add("logbook-sequence-actions-container");

                              ImageView positionalIcon = new ImageView();
                              positionalIcon.getStyleClass().add(ACTION_ICON_STYLING);

                              Label actionLabel = new Label(action.getName());
                              actionLabel.getStyleClass().add("logbook-sequence-actions-name");

                              HBox filler = new HBox();
                              HBox.setHgrow(filler, Priority.ALWAYS);

                              DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
                              Label timeElapsed = new Label(LocalTime.now().format(formatter));
                              timeElapsed
                                  .getStyleClass()
                                  .add("logbook-sequence-actions-time-elapsed");

                              sequenceAction
                                  .getChildren()
                                  .addAll(positionalIcon, actionLabel, filler, timeElapsed);

                              actionsVBox.getChildren().add(sequenceAction);
                            });

                    // Set initial positional styling to get the right order
                    actionsVBox.getChildren().stream()
                        .filter(HBox.class::isInstance)
                        .map(HBox.class::cast)
                        .forEach(
                            hbox -> {
                              ImageView imageView = (ImageView) hbox.getChildren().get(0);
                              setPositionalStyling(imageView, false);
                            });
                  });
            });

    randomizerViewModel.onActionFinished(
        action ->
            Platform.runLater(
                () -> {
                  actionsVBox.getChildren().stream()
                      .filter(HBox.class::isInstance)
                      .map(HBox.class::cast)
                      .filter(
                          hbox -> {
                            Label label = (Label) hbox.getChildren().get(1);
                            return label.getText() != null
                                && label.getText().equals(action.getName());
                          })
                      .filter(
                          hbox -> {
                            ImageView imageView = (ImageView) hbox.getChildren().get(0);
                            return !isActive(imageView);
                          })
                      .findFirst()
                      .ifPresent(
                          hbox -> {
                            ImageView imageView = (ImageView) hbox.getChildren().get(0);
                            setPositionalStyling(imageView, true);
                          });
                }));
  }

  private void setupStateListener() {
    randomizerViewModel.onStateChange(
        applicationState -> {
          Platform.runLater(
              () -> {
                if (applicationState == ApplicationState.AWAITING) {
                  cs2FocusImage.setVisible(true);
                  randomizerToggleButton.setText("Paused");
                  clearCurrentSequenceView();
                } else {
                  cs2FocusImage.setVisible(false);
                }
              });
        });
  }

  private boolean isActive(ImageView imageView) {
    return imageView.getStyleClass().stream().anyMatch(style -> style.endsWith("-active"));
  }

  private void setPositionalStyling(ImageView imageView, boolean active) {
    HBox parentHBox = (HBox) imageView.getParent();
    int index = actionsVBox.getChildren().indexOf(parentHBox);
    ObservableList<Node> children = actionsVBox.getChildren();
    ObservableList<String> styleClasses = imageView.getStyleClass();

    if (index == 0) {
      styleClasses.setAll(
          ACTION_ICON_STYLING,
          active ? START_ACTIVE_ACTION_ICON_STYLING : START_ACTION_ICON_STYLING);
    } else if (index == children.size() - 1) {
      styleClasses.setAll(
          ACTION_ICON_STYLING, active ? END_ACTIVE_ACTION_ICON_STYLING : END_ACTION_ICON_STYLING);
    } else if (index > 0 && index < children.size() - 1) {
      styleClasses.setAll(
          ACTION_ICON_STYLING,
          active ? MIDDLE_ACTIVE_ACTION_ICON_STYLING : MIDDLE_ACTION_ICON_STYLING);
    } else {
      System.err.println(
          "Ungültiger Index in setPositionalStyling: "
              + index
              + ", Listengröße: "
              + children.size());
    }
  }
}
