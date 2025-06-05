package de.bsommerfeld.randomizer.ui.view.controller;

import com.google.inject.Inject;
import de.bsommerfeld.model.ApplicationState;
import de.bsommerfeld.model.action.Action;
import de.bsommerfeld.model.action.sequence.ActionSequence;
import de.bsommerfeld.model.action.spi.ActionSequenceExecutor;
import de.bsommerfeld.randomizer.ui.view.View;
import de.bsommerfeld.randomizer.ui.view.viewmodel.RandomizerViewModel;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
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
  private final ActionSequenceExecutor actionSequenceExecutor;
  private final Map<HBox, Timer> actionTimers = new HashMap<>();
  private final Map<HBox, Long> actionStartTimes = new HashMap<>();

  @FXML private Label sequenceNameLabel;
  @FXML private VBox actionsVBox;
  @FXML private VBox historyVBox;
  @FXML private ToggleButton randomizerToggleButton;
  @FXML private ImageView cs2FocusImage;

  @Inject
  public RandomizerViewController(
      RandomizerViewModel randomizerViewModel, ActionSequenceExecutor actionSequenceExecutor) {
    this.randomizerViewModel = randomizerViewModel;
    this.actionSequenceExecutor = actionSequenceExecutor;
  }

  @FXML
  void onToggle(ActionEvent event) {
    if (!randomizerToggleButton.isSelected()) {
      randomizerViewModel.setApplicationStateToStopped();
      clearCurrentSequenceView();
      randomizerToggleButton.setText("Start");
      actionSequenceExecutor.start();
    } else {
      randomizerViewModel.setApplicationStateToRunning();
      randomizerToggleButton.setText("Stop");
      actionSequenceExecutor.stop();
    }
  }

  @FXML
  private void initialize() {
    setupBindings();
    setupListener();
    setupStateListener();
  }

  private void clearCurrentSequenceView() {
    randomizerViewModel.getCurrentActionSequenceProperty().set(null);
    actionsVBox.getChildren().clear();
    sequenceNameLabel.setText("");

    // Stop all timers
    for (Timer timer : actionTimers.values()) {
      timer.cancel();
    }
    actionTimers.clear();
    actionStartTimes.clear();
  }

  /**
   * Formats the elapsed time in seconds as "00.00s".
   *
   * @param elapsedTimeMillis the elapsed time in milliseconds
   * @return the formatted time string
   */
  private String formatElapsedTime(long elapsedTimeMillis) {
    double seconds = elapsedTimeMillis / 1000.0;
    return String.format("%05.2fs", seconds);
  }

  /**
   * Starts a timer for the given action that updates the timeElapsed label periodically.
   *
   * @param actionContainer the HBox containing the action
   * @param timeElapsedLabel the label to update
   */
  private void startActionTimer(HBox actionContainer, Label timeElapsedLabel) {
    // Cancel any existing timer for this action
    if (actionTimers.containsKey(actionContainer)) {
      actionTimers.get(actionContainer).cancel();
    }

    // Record the start time
    actionStartTimes.put(actionContainer, System.currentTimeMillis());

    // Create a new timer
    Timer timer = new Timer(true);
    actionTimers.put(actionContainer, timer);

    // Schedule a task to update the label every 100ms
    timer.scheduleAtFixedRate(
        new TimerTask() {
          @Override
          public void run() {
            long currentTime = System.currentTimeMillis();
            long startTime = actionStartTimes.getOrDefault(actionContainer, currentTime);
            long elapsedTime = currentTime - startTime;

            Platform.runLater(() -> timeElapsedLabel.setText(formatElapsedTime(elapsedTime)));
          }
        },
        0,
        100);
  }

  /**
   * Stops the timer for the given action container and updates the timeElapsed label with the final
   * time.
   *
   * @param actionContainer the HBox containing the action
   */
  private void stopActionTimerForHBox(HBox actionContainer) {
    // Get the timeElapsed label
    Label timeElapsedLabel = (Label) actionContainer.getChildren().get(3);

    // Calculate the final elapsed time
    long currentTime = System.currentTimeMillis();
    long startTime = actionStartTimes.getOrDefault(actionContainer, currentTime);
    long elapsedTime = currentTime - startTime;

    // Update the label with the final time
    timeElapsedLabel.setText(formatElapsedTime(elapsedTime));

    // Apply the finished styling
    timeElapsedLabel.getStyleClass().remove("logbook-sequence-actions-time-elapsed");
    timeElapsedLabel.getStyleClass().add("logbook-sequence-actions-time-elapsed-finished");

    // Cancel the timer
    if (actionTimers.containsKey(actionContainer)) {
      actionTimers.get(actionContainer).cancel();
      actionTimers.remove(actionContainer);
    }
  }

  private void setupBindings() {
    sequenceNameLabel.visibleProperty().bind(sequenceNameLabel.textProperty().isNotEmpty());
  }

  /** Creates the history container for the ActionSequence */
  private void createHistoryContainer(ActionSequence actionSequence) {
    if (actionSequence == null) return;

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
    setupActionSequenceFinishedListener();
    setupCurrentActionSequenceListener();
    setupActionStartListener();
    setupActionFinishedListener();
  }

  private void setupActionSequenceFinishedListener() {
    randomizerViewModel.onActionSequenceFinished(
        actionSequence -> Platform.runLater(() -> handleActionSequenceFinished(actionSequence)));
  }

  private void handleActionSequenceFinished(ActionSequence actionSequence) {
    createHistoryContainer(actionSequence);
    clearCurrentSequenceView();
  }

  private void setupCurrentActionSequenceListener() {
    randomizerViewModel
        .getCurrentActionSequenceProperty()
        .addListener(
            (_, _, newSequence) -> {
              if (newSequence == null) {
                clearCurrentSequenceView();
                return;
              }
              Platform.runLater(() -> updateCurrentSequenceView(newSequence));
            });
  }

  private void updateCurrentSequenceView(ActionSequence sequence) {
    sequenceNameLabel.setText(sequence.getName());
    actionsVBox.getChildren().clear();
    sequence
        .getActions()
        .forEach(
            action -> {
              if (action != null) {
                actionsVBox.getChildren().add(createActionRowHBox(action));
              }
            });
    applyInitialStylingToActionsInVBox();
  }

  private HBox createActionRowHBox(Action action) {
    HBox sequenceActionHBox = new HBox();
    sequenceActionHBox.getStyleClass().add("logbook-sequence-actions-container");

    ImageView positionalIcon = new ImageView();
    positionalIcon.getStyleClass().add(ACTION_ICON_STYLING);

    Label actionLabel = new Label(action.getName());
    actionLabel.getStyleClass().add("logbook-sequence-actions-name");

    HBox filler = new HBox();
    HBox.setHgrow(filler, Priority.ALWAYS);

    Label timeElapsedLabel = new Label("00.00s"); // Initialer Zeitwert
    timeElapsedLabel.getStyleClass().add("logbook-sequence-actions-time-elapsed");

    sequenceActionHBox.getChildren().addAll(positionalIcon, actionLabel, filler, timeElapsedLabel);
    return sequenceActionHBox;
  }

  private void applyInitialStylingToActionsInVBox() {
    actionsVBox.getChildren().stream()
        .filter(HBox.class::isInstance)
        .map(HBox.class::cast)
        .forEach(
            hbox -> {
              if (!hbox.getChildren().isEmpty()
                  && hbox.getChildren().getFirst() instanceof ImageView imageView) {
                setPositionalStyling(imageView, false);
              }
            });
  }

  private void setupActionStartListener() {
    randomizerViewModel.onActionStart(action -> Platform.runLater(() -> handleActionStart(action)));
  }

  private void handleActionStart(Action action) {
    findMatchingActionHBox(action.getName(), iv -> !isActive(iv))
        .ifPresent(
            hbox -> {
              if (hbox.getChildren().size() > 3
                  && hbox.getChildren().get(3) instanceof Label timeElapsedLabel) {
                startActionTimer(hbox, timeElapsedLabel);
              }
            });
  }

  private void setupActionFinishedListener() {
    randomizerViewModel.onActionFinished(
        action -> Platform.runLater(() -> handleActionFinished(action)));
  }

  private void handleActionFinished(Action action) {
    findMatchingActionHBox(action.getName(), iv -> !isActive(iv))
        .ifPresent(
            hbox -> {
              stopActionTimerForHBox(hbox);
              if (!hbox.getChildren().isEmpty()
                  && hbox.getChildren().getFirst() instanceof ImageView imageView) {
                setPositionalStyling(imageView, true);
              }
            });
  }

  /**
   * Sucht eine HBox in actionsVBox, die zu einem Aktionsnamen passt und deren ImageView einem
   * gegebenen Prädikat entspricht. Annahme: Die HBox-Struktur ist: ImageView, Label (Aktionsname),
   * Filler, Label (Zeit).
   */
  private Optional<HBox> findMatchingActionHBox(
      String actionName, java.util.function.Predicate<ImageView> imageViewPredicate) {
    return actionsVBox.getChildren().stream()
        .filter(HBox.class::isInstance)
        .map(HBox.class::cast)
        .filter(
            hbox ->
                hbox.getChildren().size() > 1
                    && hbox.getChildren().get(1) instanceof Label
                    && actionName.equals(((Label) hbox.getChildren().get(1)).getText()))
        .filter(
            hbox ->
                !hbox.getChildren().isEmpty()
                    && hbox.getChildren().get(0) instanceof ImageView
                    && imageViewPredicate.test((ImageView) hbox.getChildren().get(0)))
        .findFirst();
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
