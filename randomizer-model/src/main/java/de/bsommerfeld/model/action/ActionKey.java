package de.bsommerfeld.model.action;

import lombok.Value;

/**
 * ActionKey represents a key binding used in various actions.
 *
 * <p>Instances of ActionKey are created using a static factory method {@link #of(String)}. This
 * class is used to associate a specific key binding with an action.
 *
 * <p>The key is stored as a string and can represent any key or a special key binding like
 * "<unbound>".
 */
@Value(staticConstructor = "of")
public class ActionKey {

    String key;
}
