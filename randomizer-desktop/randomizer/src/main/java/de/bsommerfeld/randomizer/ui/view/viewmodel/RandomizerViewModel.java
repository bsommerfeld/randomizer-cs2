package de.bsommerfeld.randomizer.ui.view.viewmodel;

import com.google.inject.Inject;
import de.bsommerfeld.model.ApplicationContext;
import de.bsommerfeld.model.ApplicationState;
import de.bsommerfeld.model.action.Action;
import de.bsommerfeld.model.action.sequence.ActionSequence;
import de.bsommerfeld.model.action.sequence.ActionSequenceDispatcher;
import java.util.function.Consumer;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RandomizerViewModel {

  private final ActionSequenceDispatcher actionSequenceDispatcher;
  private final ApplicationContext applicationContext;

  @Getter
  private final ObjectProperty<ActionSequence> currentActionSequenceProperty =
      new SimpleObjectProperty<>();

  @Getter private final ObjectProperty<Action> currentActionProperty = new SimpleObjectProperty<>();

  @Inject
  public RandomizerViewModel(
      ActionSequenceDispatcher actionSequenceDispatcher, ApplicationContext applicationContext) {
    this.actionSequenceDispatcher = actionSequenceDispatcher;
    this.applicationContext = applicationContext;
    setupInternalHandler();
  }

  public void setApplicationStateToRunning() {
    applicationContext.setApplicationState(ApplicationState.RUNNING);
  }

  public void setApplicationStateToStopped() {
    applicationContext.setApplicationState(ApplicationState.IDLING);
  }

  public void onStateChange(Consumer<ApplicationState> consumer) {
    applicationContext.registerApplicationStateChangeListener(consumer);
  }

  public void onActionSequenceFinished(Consumer<ActionSequence> consumer) {
    actionSequenceDispatcher.registerSequenceFinishHandler(consumer);
  }

  public void onActionFinished(Consumer<Action> consumer) {
    actionSequenceDispatcher.registerActionFinishHandler(consumer);
  }

  private void setupInternalHandler() {
    actionSequenceDispatcher.registerSequenceHandler(currentActionSequenceProperty::set);
    actionSequenceDispatcher.registerActionHandler(currentActionProperty::set);
  }
}
