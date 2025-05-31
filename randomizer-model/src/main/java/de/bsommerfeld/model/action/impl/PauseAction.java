package de.bsommerfeld.model.action.impl;

import de.bsommerfeld.model.action.Action;
import de.bsommerfeld.model.action.ActionKey;
import de.bsommerfeld.model.config.keybind.KeyBind;

import java.util.concurrent.ThreadLocalRandom;

/**
 * The PauseAction class represents an action that pauses execution for a random duration within a specified interval.
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
        if (getInterval().isEmpty()) return;

        int min = getInterval().getMin();
        int max = getInterval().getMax();
        int delay = ThreadLocalRandom.current().nextInt(min, max);

        performInterruptibleDelay(delay);
    }

    @Override
    protected void performActionEnd(int keycode) {
        // Da bei einer Pause keine spezifische Aktion am Ende ausgeführt werden muss,
        // bleibt diese Methode leer.
    }
}
