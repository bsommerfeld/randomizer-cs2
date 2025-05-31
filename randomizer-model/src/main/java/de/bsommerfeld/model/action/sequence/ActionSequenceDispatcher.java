package de.bsommerfeld.model.action.sequence;

import com.google.inject.Inject;
import de.bsommerfeld.model.action.Action;
import de.bsommerfeld.model.action.repository.ActionRepository;
import de.bsommerfeld.model.util.FocusManager;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ActionSequenceDispatcher {

  private static final String ACTION_DISPATCHED = "Action successfully dispatched: {}";
  private static final String SEQUENCE_DISPATCHED = "ActionSequence successfully dispatched: {}";

  private final List<Consumer<ActionSequence>> sequenceHandlers = new CopyOnWriteArrayList<>();
  private final List<Consumer<Action>> actionHandlers = new CopyOnWriteArrayList<>();
  private final List<Consumer<Action>> actionFinishHandlers = new CopyOnWriteArrayList<>();
  private final List<Consumer<ActionSequence>> actionSequenceFinishHandlers =
      new CopyOnWriteArrayList<>();

  private static final List<Action> runningActions = new CopyOnWriteArrayList<>();
  private final ActionRepository actionRepository;

  @Inject
  public ActionSequenceDispatcher(ActionRepository actionRepository) {
    this.actionRepository = actionRepository;
  }

  /**
   * Dispatches a single action to all registered handlers and logs its completion.
   *
   * @param action the Action to be dispatched
   */
  private void dispatch(Action action) {
    if (action == null) return;
    if (!actionRepository.isEnabled(action)) return;
    dispatchToHandlers(action, actionHandlers);
    runningActions.add(action);
    action.execute();
    if (action.isInterrupted() && !action.hasEnded()) {
      log.info("Interrupted action processing");
      return;
    }
    finishDispatch(action);
  }

  private void finishDispatch(Action action) {
    runningActions.remove(action);
    finishActionProcessing(action);
    log.info(ACTION_DISPATCHED, action);
  }

  /**
   * Attempts to redispatch the provided action with a specified delay and handles its lifecycle
   * based on its state.
   *
   * @param action the action to be redispatched
   * @param remainingTime the delay in milliseconds before the action is processed
   */
  public void redispatch(Action action, long remainingTime) {
    action.executeWithDelay(remainingTime);
    if (action.isInterrupted() && !action.hasEnded()) {
      log.info("Interrupted action processing");
      return;
    }
    finishDispatch(action);
  }

  /**
   * Dispatches an ActionSequence to registered handlers and processes each contained Action.
   *
   * @param actionSequence the ActionSequence to be dispatched
   */
  public void dispatchSequence(ActionSequence actionSequence) {
    dispatchToHandlers(actionSequence, sequenceHandlers);
    for (Action action : actionSequence.getActions()) {
      if (!FocusManager.isCs2WindowInFocus()) { // TODO: hacky
        log.info("Interrupted sequence processing");
        return;
      }
      dispatch(action);
    }
    finishSequenceProcessing(actionSequence);
    log.info(SEQUENCE_DISPATCHED, actionSequence);
  }

  public static void discardAllRunningActions() {
    if (runningActions.isEmpty()) {
      log.info("No running actions to discard");
      return;
    }
    log.info("Discarding {} running actions", runningActions.size());
    runningActions.forEach(Action::instantInterrupt);
    runningActions.clear();
    log.info("All running actions discarded");
  }

  private void finishActionProcessing(Action action) {
    actionFinishHandlers.forEach(handler -> safeAccept(handler, action));
  }

  private void finishSequenceProcessing(ActionSequence actionSequence) {
    actionSequenceFinishHandlers.forEach(handler -> safeAccept(handler, actionSequence));
  }

  /**
   * Registers a handler to process any finished action.
   *
   * @param handler the Consumer to handle finished actions
   */
  public void registerActionFinishHandler(Consumer<Action> handler) {
    actionFinishHandlers.add(handler);
  }

  /**
   * Registers a handler to process any finished action sequence.
   *
   * @param handler the Consumer to handle finished action sequences
   */
  public void registerSequenceFinishHandler(Consumer<ActionSequence> handler) {
    actionSequenceFinishHandlers.add(handler);
  }

  /**
   * Registers a generic handler for Action events.
   *
   * @param handler the Consumer to process Action events
   */
  public void registerActionHandler(Consumer<Action> handler) {
    actionHandlers.add(handler);
  }

  /**
   * Registers a generic handler for ActionSequence events.
   *
   * @param handler the Consumer to process ActionSequence events
   */
  public void registerSequenceHandler(Consumer<ActionSequence> handler) {
    sequenceHandlers.add(handler);
  }

  private <T> void dispatchToHandlers(T item, List<Consumer<T>> handlers) {
    handlers.forEach(handler -> safeAccept(handler, item));
  }

  private <T> void safeAccept(Consumer<T> handler, T item) {
    try {
      handler.accept(item);
    } catch (Exception e) {
      log.error("Error while handling item: {}", item, e);
    }
  }
}
