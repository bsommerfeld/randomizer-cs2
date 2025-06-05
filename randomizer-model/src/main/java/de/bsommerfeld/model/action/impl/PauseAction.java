package de.bsommerfeld.model.action.impl;

import de.bsommerfeld.model.action.Action;
import de.bsommerfeld.model.action.ActionKey;
import de.bsommerfeld.model.config.keybind.KeyBind;

/**
 * The PauseAction class represents an action that pauses execution for a random duration within a
 * specified interval.
 *
 * <p>This class extends the Action class and leverages the functionality of performing
 * interruptible delays. No specific action is performed at the end of the pause duration.
 */
public class PauseAction extends Action {

  public PauseAction() {
    super("Pause", ActionKey.of(KeyBind.EMPTY_KEY_BIND.getKey()));
  }

  @Override
  protected void performActionStart(int keycode) {
    // No action needed at start - the delay is handled by executeWithDelay()
    // Removing the duplicate performInterruptibleDelay call here
  }

  @Override
  protected void performActionEnd(int keycode) {
    // Since no specific action needs to be executed at the end of a pause,
    // this method remains empty.
  }
}
