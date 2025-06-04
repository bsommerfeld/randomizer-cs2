package de.bsommerfeld.model.action.spi;

/**
 * Interface for executing actions.
 * This interface defines the contract for classes that can execute actions like keyboard, mouse, or custom actions.
 */
public interface ActionExecutor {

    /**
     * Executes the start of an action with the specified key code.
     *
     * @param keyCode the code of the key to be pressed or action to be started
     */
    void executeActionStart(int keyCode);

    /**
     * Executes the end of an action with the specified key code.
     *
     * @param keyCode the code of the key to be released or action to be ended
     */
    void executeActionEnd(int keyCode);
}