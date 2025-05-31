package de.bsommerfeld.model.action.repository;

import de.bsommerfeld.model.action.Action;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/** Haelt die Grundinstanzen der Aktionen mit den jeweils zugewiesenen Keybinds. */
@Slf4j
public class ActionRepository {

  private final Map<Action, Boolean> actions = new LinkedHashMap<>();

  /**
   * Registers a new action in the repository by adding it to the internal map of actions and saving
   * the state.
   *
   * @param action The action to be registered and enabled. The action will be added to the
   *     repository and its state will be persisted immediately.
   */
  public void register(Action action) {
    actions.put(action, true);
  }

  public boolean hasActionWithName(String name) {
    return actions.keySet().stream().anyMatch(action -> action.getName().equals(name));
  }

  /**
   * Unregisters the given action, removing it from the repository and logging the unregistration
   * event.
   *
   * @param action The action to be unregistered.
   */
  public void unregister(Action action) {
    actions.remove(action);
  }

  /**
   * Enables the specified action.
   *
   * @param action The action to be enabled.
   */
  public void enable(Action action) {
    actions.put(action, true);
  }

  /**
   * Disables the specified action by setting its state to false in the internal actions map and
   * saving the updated states to persistent storage.
   *
   * @param action the Action to be disabled.
   */
  public void disable(Action action) {
    actions.put(action, false);
  }

  /**
   * Checks if the specified action is currently enabled.
   *
   * @param action The action to check for its enabled state.
   * @return true if the action is enabled or if it is not found in the repository, false otherwise.
   */
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
  public Map<Action, Boolean> getActions() {
    Map<Action, Boolean> actions = new LinkedHashMap<>();
    this.actions.forEach(
        (action, enabled) -> {
          try {
            actions.put(action.clone(), enabled);
          } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
          }
        });
    return actions;
  }

  /**
   * Retrieves an Action by its name and returns a cloned copy of it.
   *
   * @param actionName the name of the action to retrieve
   * @return a cloned copy of the Action with the specified name
   * @throws IllegalArgumentException if no action with the specified name is found
   * @throws RuntimeException if the action cannot be cloned
   */
  public Action getByName(String actionName) {
    Action originalAction =
        actions.keySet().stream()
            .filter(action -> action.getName().equals(actionName))
            .findFirst()
            .orElseThrow();

    try {
      return originalAction.clone();
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException("Cloning not supported for action: " + actionName, e);
    }
  }
}
