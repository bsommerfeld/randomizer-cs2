package de.bsommerfeld.model.action.repository;

import com.google.inject.Singleton;
import de.bsommerfeld.model.action.Action;
import de.bsommerfeld.model.action.spi.ActionRepository;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Default implementation of the ActionRepository interface.
 * This class manages actions and their enabled/disabled state.
 */
@Slf4j
@Singleton
public class DefaultActionRepository implements ActionRepository {

    private final Map<Action, Boolean> actions = new LinkedHashMap<>();

    /**
     * Registers a new action in the repository by adding it to the internal map of actions.
     *
     * @param action The action to be registered and enabled.
     */
    @Override
    public void register(Action action) {
        actions.put(action, true);
    }

    /**
     * Checks if an action with the specified name exists in the repository.
     *
     * @param name The name of the action to check.
     * @return true if an action with the specified name exists, false otherwise.
     */
    @Override
    public boolean hasActionWithName(String name) {
        return actions.keySet().stream().anyMatch(action -> action.getName().equals(name));
    }

    /**
     * Unregisters the given action, removing it from the repository.
     *
     * @param action The action to be unregistered.
     */
    @Override
    public void unregister(Action action) {
        actions.remove(action);
    }

    /**
     * Enables the specified action.
     *
     * @param action The action to be enabled.
     */
    @Override
    public void enable(Action action) {
        actions.put(action, true);
    }

    /**
     * Disables the specified action.
     *
     * @param action The action to be disabled.
     */
    @Override
    public void disable(Action action) {
        actions.put(action, false);
    }

    /**
     * Checks if the specified action is currently enabled.
     *
     * @param action The action to check for its enabled state.
     * @return true if the action is enabled or if it is not found in the repository, false otherwise.
     */
    @Override
    public boolean isEnabled(Action action) {
        return actions.getOrDefault(action, true);
    }

    /**
     * Retrieves a map of registered actions along with their enabled/disabled state.
     *
     * @return a Map containing {@link Action} objects as keys and their corresponding Boolean state
     *     indicating whether the action is enabled (true) or disabled (false).
     * @throws RuntimeException if cloning an action fails.
     */
    @Override
    public Map<Action, Boolean> getActions() {
        Map<Action, Boolean> actionsCopy = new LinkedHashMap<>();
        this.actions.forEach(
            (action, enabled) -> {
                try {
                    actionsCopy.put(action.clone(), enabled);
                } catch (CloneNotSupportedException e) {
                    throw new RuntimeException("Failed to clone action", e);
                }
            });
        return actionsCopy;
    }

    /**
     * Retrieves an Action by its name and returns a cloned copy of it.
     *
     * @param actionName the name of the action to retrieve
     * @return a cloned copy of the Action with the specified name
     * @throws IllegalArgumentException if no action with the specified name is found
     * @throws RuntimeException if the action cannot be cloned
     */
    @Override
    public Action getByName(String actionName) {
        Action originalAction =
            actions.keySet().stream()
                .filter(action -> action.getName().equals(actionName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No action found with name: " + actionName));

        try {
            return originalAction.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("Failed to clone action: " + actionName, e);
        }
    }
}