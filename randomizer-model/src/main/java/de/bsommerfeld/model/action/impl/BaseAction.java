package de.bsommerfeld.model.action.impl;

import de.bsommerfeld.model.action.Action;
import de.bsommerfeld.model.action.ActionKey;
import lombok.extern.slf4j.Slf4j;

/**
 * BaseAction is a specific implementation of the Action class designed to handle different types of user actions.
 * Depending on the action type, it can simulate mouse presses, mouse wheel movements, and key presses.
 */
@Slf4j
public class BaseAction extends Action {

    public BaseAction(String name, ActionKey actionKey) {
        super(name, actionKey);
    }

    @Override
    protected void performActionStart(int keyCode) {
        switch (getActionType()) {
            case MOUSE -> KNUFFI.mousePress(keyCode);
            case MOUSE_WHEEL -> KNUFFI.mouseWheel(keyCode);
            case KEYBOARD -> KNUFFI.keyPress(keyCode);
        }
    }

    @Override
    protected void performActionEnd(int keyCode) {
        switch (getActionType()) {
            case MOUSE -> KNUFFI.mouseRelease(keyCode);
            case KEYBOARD -> KNUFFI.keyRelease(keyCode);
        }
    }
}
