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

  private final RandomizerViewModel randomizerViewModel;

  @FXML private Label sequenceNameLabel;
  @FXML private VBox actionsVBox;
  @FXML private VBox historyVBox;
  @FXML private ToggleButton randomizerToggleButton;
  @FXML private Label runnerStateLabel;
  @FXML private Label cs2FocusLabel;
  @FXML private HBox logbookState;

  @Inject
  public RandomizerViewController(RandomizerViewModel randomizerViewModel) {
    this.randomizerViewModel = randomizerViewModel;
  }

  @FXML
  void onToggle(ActionEvent event) {
    if (!randomizerToggleButton.isSelected()) {
      randomizerViewModel.setApplicationStateToStopped();
    } else {
      randomizerViewModel.setApplicationStateToRunning();
    }
  }

  @FXML
  private void initialize() {
    setupBindings();
    setupListener();
    setupStateListener();
  }

  private void setupBindings() {
    sequenceNameLabel.visibleProperty().bind(sequenceNameLabel.textProperty().isNotEmpty());
  }

  /** Creates the history container for the ActionSequence */
  private void createActionSequenceContainer(ActionSequence actionSequence) {
    HBox container = new HBox();
    container.getStyleClass().add("logbook-history-entry-container");

    Label actionSequenceNameLabel = new Label(actionSequence.getName());
    actionSequenceNameLabel.getStyleClass().add("logbook-history-entry-name");

    HBox centerBox = new HBox();
    HBox.setHgrow(centerBox, Priority.ALWAYS);
    centerBox.getStyleClass().add("logbook-history-entry-rightbox");
    Label actionSequenceActionCount = new Label(actionSequence.getActions().size() + " Actions");
    actionSequenceActionCount.getStyleClass().add("logbook-history-entry-action-count");

    HBox rigfhtFiller = new HBox();
    HBox.setHgrow(rigfhtFiller, Priority.ALWAYS);

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
    Label actionSequenceActionExecutedAt = new Label(LocalTime.now().format(formatter));
    actionSequenceActionExecutedAt.getStyleClass().add("logbook-history-entry-executed-at");
    centerBox.getChildren().addAll(actionSequenceActionCount, rigfhtFiller, actionSequenceActionExecutedAt);

    HBox leftFiller = new HBox();
    HBox.setHgrow(leftFiller, Priority.ALWAYS);


    container
        .getChildren()
        .addAll(actionSequenceNameLabel, leftFiller, centerBox);
    historyVBox.getChildren().addFirst(container);
  }

  private void setupListener() {
    randomizerViewModel.onActionSequenceFinished(
        actionSequence ->
            Platform.runLater(
                () -> {
                  createActionSequenceContainer(actionSequence);
                  sequenceNameLabel.setText("");
                  actionsVBox.getChildren().clear();
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
                              sequenceAction.getStyleClass().add("logbook-sequence-actions-container");

                              ImageView positionalIcon = new ImageView();
                              positionalIcon.getStyleClass().add("logbook-sequence-actions-icon");

                              Label actionLabel = new Label(action.getName());
                              actionLabel.getStyleClass().add("logbook-sequence-actions-name");

                              HBox filler = new HBox();
                              HBox.setHgrow(filler, Priority.ALWAYS);

                              DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
                              Label timeElapsed = new Label(LocalTime.now().format(formatter));
                              timeElapsed.getStyleClass().add("logbook-sequence-actions-time-elapsed");


                              sequenceAction.getChildren().addAll(positionalIcon, actionLabel, filler, timeElapsed);

                              actionsVBox.getChildren().add(sequenceAction);

                            });
//                    Platform.runLater(
//                        () -> {
//                          actionsVBox
//                              .getChildren()
//                              .forEach(label -> setPositionalStyling((Label) label, false));
//                        });
                  });
            });

    randomizerViewModel.onActionFinished(
        action ->
            Platform.runLater(
                () -> {
                  actionsVBox.getChildren().stream()
                      .filter(Label.class::isInstance)
                      .map(Label.class::cast)
                      .filter(label -> label.getText() != null)
                      .filter(label -> label.getText().equals(action.getName()))
                      .filter(label -> !isActive(label))
                      .findFirst()
                      .ifPresent(label -> setPositionalStyling(label, true));
                }));
  }

  private void setupStateListener() {
    randomizerViewModel.onStateChange(
        applicationState -> {
          if (applicationState == ApplicationState.AWAITING) {
            runnerStateLabel.setVisible(true);
            cs2FocusLabel.setVisible(true);
            logbookState.getStyleClass().add("logbook-state-awaiting");
          } else {
            runnerStateLabel.setVisible(false);
            cs2FocusLabel.setVisible(false);
            logbookState.getStyleClass().remove("logbook-state-awaiting");
          }
        });
  }

  private boolean isActive(Label label) {
    return label.getStyleClass().stream().anyMatch(style -> style.endsWith("-active"));
  }

  private void setPositionalStyling(Label label, boolean active) {
    int index = actionsVBox.getChildren().indexOf(label);
    ObservableList<Node> children = actionsVBox.getChildren();
    ObservableList<String> styleClasses = label.getStyleClass();

    if (index == 0) {
      styleClasses.setAll(
          ACTION_NAME_STYLING,
          active ? START_ACTIVE_ACTION_NAME_STYLING : START_ACTION_NAME_STYLING);
    } else if (index == children.size() - 1) {
      styleClasses.setAll(
          ACTION_NAME_STYLING, active ? END_ACTIVE_ACTION_NAME_STYLING : END_ACTION_NAME_STYLING);
    } else if (index > 0 && index < children.size() - 1) {
      styleClasses.setAll(
          ACTION_NAME_STYLING,
          active ? MIDDLE_ACTIVE_ACTION_NAME_STYLING : MIDDLE_ACTION_NAME_STYLING);
    } else {
      System.err.println(
          "Ungültiger Index in setPositionalStyling: "
              + index
              + ", Listengröße: "
              + children.size());
    }
  }
}
