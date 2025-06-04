package de.bsommerfeld.model.action.sequence;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.bsommerfeld.model.action.Action;
import de.bsommerfeld.model.action.spi.ActionRepository;
import de.bsommerfeld.model.action.spi.ActionSequenceDispatcher;
import de.bsommerfeld.model.action.spi.FocusManager;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Default implementation of the ActionSequenceDispatcher interface.
 * This class dispatches actions and action sequences to registered handlers.
 */
@Slf4j
@Singleton
public class DefaultActionSequenceDispatcher implements ActionSequenceDispatcher {

    private static final String ACTION_DISPATCHED = "Action successfully dispatched: {}";
    private static final String SEQUENCE_DISPATCHED = "ActionSequence successfully dispatched: {}";

    private final List<Consumer<ActionSequence>> sequenceHandlers = new CopyOnWriteArrayList<>();
    private final List<Consumer<Action>> actionHandlers = new CopyOnWriteArrayList<>();
    private final List<Consumer<Action>> actionFinishHandlers = new CopyOnWriteArrayList<>();
    private final List<Consumer<ActionSequence>> actionSequenceFinishHandlers = new CopyOnWriteArrayList<>();

    private final List<Action> runningActions = new CopyOnWriteArrayList<>();
    private final ActionRepository actionRepository;
    private final FocusManager focusManager;

    @Inject
    public DefaultActionSequenceDispatcher(ActionRepository actionRepository, FocusManager focusManager) {
        this.actionRepository = actionRepository;
        this.focusManager = focusManager;
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
     * Redispatches an action with a specified delay.
     *
     * @param action the action to be redispatched
     * @param remainingTime the delay in milliseconds before the action is processed
     */
    @Override
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
    @Override
    public void dispatchSequence(ActionSequence actionSequence) {
        dispatchToHandlers(actionSequence, sequenceHandlers);
        for (Action action : actionSequence.getActions()) {
            if (!focusManager.isApplicationWindowInFocus()) {
                log.info("Interrupted sequence processing due to loss of focus");
                return;
            }
            dispatch(action);
        }
        finishSequenceProcessing(actionSequence);
        log.info(SEQUENCE_DISPATCHED, actionSequence);
    }

    /**
     * Discards all running actions.
     */
    @Override
    public void discardAllRunningActions() {
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