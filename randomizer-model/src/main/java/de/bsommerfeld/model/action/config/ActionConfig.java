package de.bsommerfeld.model.action.config;

import com.google.inject.Singleton;
import lombok.Getter;
import lombok.Setter;

/**
 * Configuration parameters for the action package.
 * This class centralizes all configuration parameters used in the action package,
 * making it easier to modify and maintain them.
 */
@Getter
@Setter
@Singleton
public class ActionConfig {

    /**
     * The interval in milliseconds for checking if an action should be interrupted.
     */
    private int interruptCheckInterval = 50;

    /**
     * The number of steps to use when performing a smooth mouse movement.
     */
    private int mouseMoveSteps = 50;

    /**
     * The maximum distance in pixels that a mouse can move in a single action.
     */
    private int maxMouseMoveDistance = 5000;

    /**
     * The interval in milliseconds for checking if the application window is in focus.
     */
    private int focusCheckInterval = 500;

    /**
     * The minimum wait time in milliseconds between action sequence executions.
     */
    private int minWaitTime = 30 * 1000;

    /**
     * The maximum wait time in milliseconds between action sequence executions.
     */
    private int maxWaitTime = 120 * 1000;

    /**
     * The delay in milliseconds between mouse move steps.
     */
    private int mouseMoveSmoothDelay = 10;
}