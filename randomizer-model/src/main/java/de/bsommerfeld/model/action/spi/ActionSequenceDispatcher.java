package de.bsommerfeld.model.action.spi;

import de.bsommerfeld.model.action.Action;
import de.bsommerfeld.model.action.sequence.ActionSequence;

import java.util.function.Consumer;

/**
 * Interface for dispatching actions and action sequences.
 * This interface defines the contract for classes that dispatch actions and action sequences to handlers.
 */
public interface ActionSequenceDispatcher {

    /**
     * Redispatches an action with a specified delay.
     *
     * @param action the action to be redispatched
     * @param remainingTime the delay in milliseconds before the action is processed
     */
    void redispatch(Action action, long remainingTime);

    /**
     * Dispatches an ActionSequence to registered handlers and processes each contained Action.
     *
     * @param actionSequence the ActionSequence to be dispatched
     */
    void dispatchSequence(ActionSequence actionSequence);

    /**
     * Discards all running actions.
     */
    void discardAllRunningActions();

    /**
     * Registers a handler to process any finished action.
     *
     * @param handler the Consumer to handle finished actions
     */
    void registerActionFinishHandler(Consumer<Action> handler);

    /**
     * Registers a handler to process any finished action sequence.
     *
     * @param handler the Consumer to handle finished action sequences
     */
    void registerSequenceFinishHandler(Consumer<ActionSequence> handler);

    /**
     * Registers a generic handler for Action events.
     *
     * @param handler the Consumer to process Action events
     */
    void registerActionHandler(Consumer<Action> handler);

    /**
     * Registers a generic handler for ActionSequence events.
     *
     * @param handler the Consumer to process ActionSequence events
     */
    void registerSequenceHandler(Consumer<ActionSequence> handler);
}