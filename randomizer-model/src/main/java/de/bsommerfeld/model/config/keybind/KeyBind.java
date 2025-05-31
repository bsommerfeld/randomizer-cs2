package de.bsommerfeld.model.config.keybind;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@AllArgsConstructor
@Setter
@Getter
@EqualsAndHashCode(of = "key")
@ToString
public class KeyBind {

    /**
     * A KeyBind object representing an unbound key action.
     *
     * <p>This constant is used to signify that no key is assigned to a specific action. The key is
     * set to "<unbound>" and the action is set to "none".
     */
    public static final KeyBind EMPTY_KEY_BIND = new KeyBind("<unbound>", "none");

    /**
     * The key associated with a specific action in the KeyBind configuration. This string represents the actual
     * keybinding that triggers the corresponding action.
     */
    private String key;
    /**
     * Represents the action associated with a specific key binding. This variable holds the action name or description
     * that is triggered when the corresponding key is pressed.
     */
    private String action;
}
