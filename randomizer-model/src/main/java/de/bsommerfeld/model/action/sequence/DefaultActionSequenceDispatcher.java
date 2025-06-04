package de.bsommerfeld.model.action.sequence;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.bsommerfeld.model.ApplicationContext;
import de.bsommerfeld.model.action.Action;
import de.bsommerfeld.model.action.spi.ActionRepository;
import de.bsommerfeld.model.action.spi.ActionSequenceDispatcher;
import de.bsommerfeld.model.action.spi.FocusManager;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;

/**
 * Default implementation of the ActionSequenceDispatcher interface. This class dispatches actions
 * and action sequences to registered handlers.
 */
@Slf4j
@Singleton
public class DefaultActionSequenceDispatcher implements ActionSequenceDispatcher {

  private static final String ACTION_DISPATCHED = "Action successfully dispatched: {}";
  private static final String SEQUENCE_DISPATCHED = "ActionSequence successfully dispatched: {}";

  private final List<Consumer<ActionSequence>> sequenceHandlers = new CopyOnWriteArrayList<>();
  private final List<Consumer<Action>> actionHandlers = new CopyOnWriteArrayList<>();
  private final List<Consumer<Action>> actionFinishHandlers = new CopyOnWriteArrayList<>();
  private final List<Consumer<ActionSequence>> actionSequenceFinishHandlers =
      new CopyOnWriteArrayList<>();

  private final List<Action> runningActions = new CopyOnWriteArrayList<>();
  private final ActionRepository actionRepository;
  private final FocusManager focusManager;
  private final ApplicationContext applicationContext;
  private volatile ActionSequence currentSequence = null;

  @Inject
  public DefaultActionSequenceDispatcher(
      ActionRepository actionRepository,
      FocusManager focusManager,
      ApplicationContext applicationContext) {
    this.actionRepository = actionRepository;
    this.focusManager = focusManager;
    this.applicationContext = applicationContext;
  }

  /**
   * Dispatches a single action to all registered handlers and logs its completion.
   *
   * @param action the Action to be dispatched
   */
  private void dispatch(Action action) {
    if (action == null) return;
    if (!actionRepository.isEnabled(action)) return;

    // Check if the current sequence is interrupted
    if (currentSequence != null && currentSequence.isInterrupted()) {
      log.info("Skipping dispatch of action {} because sequence is interrupted", action);
      return;
    }

    try {
      dispatchToHandlers(action, actionHandlers);
      runningActions.add(action);

      action.execute();

      if (action.isInterrupted()) {
        log.info("Interrupted action processing for {}", action);
        return;
      }

      finishDispatch(action);
    } catch (Exception e) {
      log.error("Error dispatching action {}", action, e);
      // Ensure action is removed from running actions even if an exception occurs
      runningActions.remove(action);
      // If this action is part of a sequence, mark the sequence as interrupted
      if (currentSequence != null) {
        currentSequence.interrupt();
      }
      throw e;
    }
  }

  private void finishDispatch(Action action) {
    runningActions.remove(action);
    finishActionProcessing(action);
    log.info(ACTION_DISPATCHED + " ({} ms)", action, action.getDelay());
  }

  /**
   * Redispatches an action with a specified delay.
   *
   * @param action the action to be redispatched
   * @param remainingTime the delay in milliseconds before the action is processed
   */
  @Override
  public void redispatch(Action action, long remainingTime) {
    // Check if the current sequence is interrupted
    if (currentSequence != null && currentSequence.isInterrupted()) {
      log.info("Skipping redispatch of action {} because sequence is interrupted", action);
      return;
    }

    // Pre-execution check: Don't execute if already interrupted
    if (action.isInterrupted()) {
      log.info("Action {} has been interrupted and will not be re-executed.", action);
      return;
    }

    try {
      runningActions.add(action);
      action.executeWithDelay(remainingTime);

      // Post-execution check: Don't finish if interrupted during execution
      if (action.isInterrupted()) {
        log.info("Interrupted action processing during redispatch");
        // If this action is part of a sequence, mark the sequence as interrupted
        if (currentSequence != null) {
          currentSequence.interrupt();
        }
        return;
      }

      finishDispatch(action);
    } catch (Exception e) {
      log.error("Error redispatching action {}", action, e);
      // Ensure action is removed from running actions even if an exception occurs
      runningActions.remove(action);
      // If this action is part of a sequence, mark the sequence as interrupted
      if (currentSequence != null) {
        currentSequence.interrupt();
      }
      throw e;
    }
  }

  /**
   * Dispatches an ActionSequence to registered handlers and processes each contained Action.
   *
   * @param actionSequence the ActionSequence to be dispatched
   */
  @Override
  public void dispatchSequence(ActionSequence actionSequence) {
    if (actionSequence == null) return;
    if (!actionSequence.isActive()) return;

    // Discard any currently running actions before starting a new sequence
    discardAllRunningActions();

    // Reset any previous interrupted state
    actionSequence.resetInterrupted();

    // Set as current sequence
    currentSequence = actionSequence;

    dispatchToHandlers(actionSequence, sequenceHandlers);

    try {
      for (Action action : actionSequence.getActions()) {
        // Check for focus loss
        if (applicationContext.isCheckForCS2Focus() && !focusManager.isApplicationWindowInFocus()) {
          log.info("Interrupted sequence processing due to loss of focus");
          actionSequence.interrupt();
          break;
        }

        // Check if sequence was interrupted
        if (actionSequence.isInterrupted()) {
          log.info(
              "Sequence {} was interrupted, stopping further action processing",
              actionSequence.getName());
          break;
        }

        // Skip already interrupted actions
        if (action.isInterrupted()) {
          log.info("Skipped action {} because it was interrupted before executing", action);
          continue;
        }

        dispatch(action);

        // Check if action execution caused sequence interruption
        if (actionSequence.isInterrupted()) {
          log.info(
              "Sequence {} was interrupted during action execution, stopping further processing",
              actionSequence.getName());
          break;
        }
      }

      // Only finish processing if the sequence wasn't interrupted
      if (!actionSequence.isInterrupted()) {
        finishSequenceProcessing(actionSequence);
        log.info(SEQUENCE_DISPATCHED, actionSequence);
      } else {
        log.info(
            "Sequence {} was not fully dispatched due to interruption", actionSequence.getName());
      }
    } finally {
      // Clear current sequence reference
      discardAllRunningActions();
      currentSequence.resetInterrupted();
      currentSequence = null;
    }
  }

  /** Discards all running actions and interrupts the current sequence if one exists. */
  @Override
  public void discardAllRunningActions() {
    // First check if we have a current sequence and interrupt it
    if (currentSequence != null) {
      log.info("Interrupting current sequence: {}", currentSequence.getName());
      currentSequence.instantInterrupt();
    }

    // Then handle any individual running actions
    if (runningActions.isEmpty()) {
      log.info("No individual running actions to discard");
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
    // Make sure all actions are in a normalized state
    actionSequence.getActions().forEach(Action::normalize);

    // Make sure the sequence itself is not marked as interrupted
    if (actionSequence.isInterrupted()) {
      log.info(
          "Resetting interrupted state for sequence {} before finishing", actionSequence.getName());
      actionSequence.resetInterrupted();
    }

    // Notify all handlers that the sequence is finished
    actionSequenceFinishHandlers.forEach(handler -> safeAccept(handler, actionSequence));

    log.debug("Sequence processing finished for {}", actionSequence.getName());
  }

  /**
   * Registers a handler to process any finished action.
   *
   * @param handler the Consumer to handle finished actions
   */
  @Override
  public void registerActionFinishHandler(Consumer<Action> handler) {
    actionFinishHandlers.add(handler);
  }

  /**
   * Registers a handler to process any finished action sequence.
   *
   * @param handler the Consumer to handle finished action sequences
   */
  @Override
  public void registerSequenceFinishHandler(Consumer<ActionSequence> handler) {
    actionSequenceFinishHandlers.add(handler);
  }

  /**
   * Registers a generic handler for Action events.
   *
   * @param handler the Consumer to process Action events
   */
  @Override
  public void registerActionHandler(Consumer<Action> handler) {
    actionHandlers.add(handler);
  }

  /**
   * Registers a generic handler for ActionSequence events.
   *
   * @param handler the Consumer to process ActionSequence events
   */
  @Override
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
