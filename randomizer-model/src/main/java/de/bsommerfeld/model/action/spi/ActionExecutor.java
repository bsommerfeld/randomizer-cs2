package de.bsommerfeld.model.action.spi;

import de.bsommerfeld.model.action.ActionType;

/**
 * Interface for executing actions. This interface defines the contract for classes that can execute
 * actions like keyboard, mouse, or custom actions.
 */
public interface ActionExecutor {

  /**
   * Executes the start of an action with the specified key code and action type.
   *
   * @param keyCode the code of the key to be pressed or the identifier for the action
   * @param actionType the type of the action to be executed, represented by an {@link ActionType}
   */
  void executeActionStart(int keyCode, ActionType actionType);

  /**
   * Executes the end of an action with the specified key code.
   *
   * @param keyCode the code of the key to be released or action to be ended
   * @param actionType the type of the action to be ended, represented by an {@link ActionType}
   */
  void executeActionEnd(int keyCode, ActionType actionType);
}
