package de.bsommerfeld.model.action.spi;

import de.bsommerfeld.model.action.Action;

import java.util.Map;

/**
 * Interface for repositories that manage actions.
 * This interface defines the contract for classes that store and retrieve actions.
 */
public interface ActionRepository {

    /**
     * Registers a new action in the repository.
     *
     * @param action The action to be registered and enabled.
     */
    void register(Action action);

    /**
     * Checks if an action with the specified name exists in the repository.
     *
     * @param name The name of the action to check.
     * @return true if an action with the specified name exists, false otherwise.
     */
    boolean hasActionWithName(String name);

    /**
     * Unregisters the given action, removing it from the repository.
     *
     * @param action The action to be unregistered.
     */
    void unregister(Action action);

    /**
     * Enables the specified action.
     *
     * @param action The action to be enabled.
     */
    void enable(Action action);

    /**
     * Disables the specified action.
     *
     * @param action The action to be disabled.
     */
    void disable(Action action);

    /**
     * Checks if the specified action is currently enabled.
     *
     * @param action The action to check for its enabled state.
     * @return true if the action is enabled or if it is not found in the repository, false otherwise.
     */
    boolean isEnabled(Action action);

    /**
     * Retrieves a map of registered actions along with their enabled/disabled state.
     *
     * @return a Map containing {@link Action} objects as keys and their corresponding Boolean state
     *     indicating whether the action is enabled (true) or disabled (false).
     */
    Map<Action, Boolean> getActions();

    /**
     * Retrieves an Action by its name and returns a cloned copy of it.
     *
     * @param actionName the name of the action to retrieve
     * @return a cloned copy of the Action with the specified name
     * @throws IllegalArgumentException if no action with the specified name is found
     */
    Action getByName(String actionName);
}