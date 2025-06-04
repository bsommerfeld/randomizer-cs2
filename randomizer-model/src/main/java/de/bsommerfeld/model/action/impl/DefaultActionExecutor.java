package de.bsommerfeld.model.action.impl;

import com.google.inject.Singleton;
import de.bsommerfeld.model.action.ActionType;
import de.bsommerfeld.model.action.spi.ActionExecutor;
import lombok.extern.slf4j.Slf4j;

import java.awt.*;

/**
 * Default implementation of the ActionExecutor interface.
 * This class uses a Robot to execute keyboard and mouse actions.
 */
@Slf4j
@Singleton
public class DefaultActionExecutor implements ActionExecutor {

    private final Robot robot;

    /**
     * Creates a new DefaultActionExecutor with a Robot instance.
     *
     * @throws RuntimeException if the Robot cannot be created
     */
    public DefaultActionExecutor() {
        try {
            this.robot = new Robot();
        } catch (AWTException e) {
            log.error("Failed to create Robot instance", e);
            throw new RuntimeException("Failed to create Robot instance", e);
        }
    }

    /**
     * Executes the start of an action with the specified key code.
     *
     * @param keyCode the code of the key to be pressed or action to be started
     */
    @Override
    public void executeActionStart(int keyCode) {
        ActionType actionType = determineActionType(keyCode);
        switch (actionType) {
            case MOUSE -> robot.mousePress(keyCode);
            case MOUSE_WHEEL -> robot.mouseWheel(keyCode);
            case KEYBOARD -> robot.keyPress(keyCode);
            default -> log.debug("No action to execute for key code: {}", keyCode);
        }
    }

    /**
     * Executes the end of an action with the specified key code.
     *
     * @param keyCode the code of the key to be released or action to be ended
     */
    @Override
    public void executeActionEnd(int keyCode) {
        ActionType actionType = determineActionType(keyCode);
        switch (actionType) {
            case MOUSE -> robot.mouseRelease(keyCode);
            case KEYBOARD -> robot.keyRelease(keyCode);
            default -> log.debug("No action to end for key code: {}", keyCode);
        }
    }

    /**
     * Determines the action type based on the key code.
     *
     * @param keyCode the key code to determine the action type for
     * @return the action type
     */
    private ActionType determineActionType(int keyCode) {
        if (isMouseWheelCode(keyCode)) {
            return ActionType.MOUSE_WHEEL;
        } else if (isMouseCode(keyCode)) {
            return ActionType.MOUSE;
        } else if (isKeyboardCode(keyCode)) {
            return ActionType.KEYBOARD;
        } else {
            return ActionType.CUSTOM;
        }
    }

    private boolean isMouseWheelCode(int keyCode) {
        return keyCode == 1 || keyCode == -1; // Up or down wheel
    }

    private boolean isMouseCode(int keyCode) {
        // Mouse button codes are typically in the range of InputEvent.getMaskForButton(1-5)
        return keyCode >= 16 && keyCode <= 256; // Approximate range for mouse buttons
    }

    private boolean isKeyboardCode(int keyCode) {
        // Keyboard key codes are typically in the range of KeyEvent.VK_*
        return keyCode >= 0 && keyCode <= 65535 && !isMouseCode(keyCode);
    }
}