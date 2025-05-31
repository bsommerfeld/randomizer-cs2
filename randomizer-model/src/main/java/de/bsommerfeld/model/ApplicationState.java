package de.bsommerfeld.model;

/**
 * The {@code ApplicationState} enum represents various states that an application can be in.
 */
public enum ApplicationState {

    /**
     * Represents the state where the application is actively running and operational.
     */
    RUNNING,
    /**
     * The {@code AWAITING} state indicates that the application is in a waiting phase. This could be due to various
     * reasons such as waiting for user input, an external event, or a specific condition to be met before proceeding to
     * the next state.
     */
    AWAITING,
    /**
     * The {@code CORRUPTED} state indicates that the application has encountered an error or unexpected condition
     * rendering it unusable or unstable.
     */
    CORRUPTED,
    /**
     * Represents the state where the application is not currently executing tasks but is still active and ready to
     * perform actions upon request.
     */
    IDLING
}
