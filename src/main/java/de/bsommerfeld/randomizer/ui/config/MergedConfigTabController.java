package de.bsommerfeld.randomizer.ui.config;

import de.bsommerfeld.randomizer.config.keybinds.KeybindConfig;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;

import java.util.Optional;

/**
 * Shows the keybinds CS2 ends up with. CS2 loads the default config first and the custom keybinds
 * on top, so for the same key the custom entry wins. The main controller hands in what the two
 * config tabs show.
 */
public final class MergedConfigTabController {

    /** The explaining line on top and the JSON below it. */
    record View(String status, String json) {
    }

    @FXML private Label statusLabel;
    @FXML private TextArea jsonArea;

    public void show(Optional<KeybindConfig> defaults, Optional<KeybindConfig> custom) {
        View view = merge(defaults, custom);
        statusLabel.setText(view.status());
        jsonArea.setText(view.json());
    }

    /** Both configs merged. With one of them missing the other stands alone and the status says so. */
    static View merge(Optional<KeybindConfig> defaults, Optional<KeybindConfig> custom) {
        if (defaults.isPresent() && custom.isPresent()) {
            return new View("CS2 loads the default keybinds first and lays the custom keybinds over them. "
                    + "For the same key the custom entry wins, \"<unbound>\" means the key has no binding left.",
                    KeybindConfig.displayJson(defaults.get().model().mergedWith(custom.get().model())));
        }
        if (defaults.isPresent()) {
            return new View("Custom keybinds not loaded. Only the defaults apply.",
                    defaults.get().prettyJson());
        }
        if (custom.isPresent()) {
            return new View("Default keybinds not loaded. Only the custom keybinds are shown.",
                    custom.get().prettyJson());
        }
        return new View("No config loaded. Load them in the two tabs next to this one.", "");
    }
}
