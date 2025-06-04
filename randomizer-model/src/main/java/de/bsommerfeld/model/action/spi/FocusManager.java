package de.bsommerfeld.model.action.spi;

/**
 * Interface for managing application focus.
 * This interface defines the contract for classes that check if the application window is in focus.
 */
public interface FocusManager {

    /**
     * Checks if the application window is in focus.
     *
     * @return true if the application window is in focus, false otherwise
     */
    boolean isApplicationWindowInFocus();
}