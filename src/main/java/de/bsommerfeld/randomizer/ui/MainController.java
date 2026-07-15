package de.bsommerfeld.randomizer.ui;

import de.bsommerfeld.randomizer.config.ConfigRepository;
import de.bsommerfeld.randomizer.config.keybinds.KeybindConfig;
import de.bsommerfeld.randomizer.ui.config.ConfigTabController;
import javafx.fxml.FXML;

/**
 * The application shell: composes the tabs (each an {@code fx:include} with its own controller)
 * and binds the two keybind-config tabs to their repositories. All tab behavior lives in the
 * included controllers.
 */
public final class MainController {

    @FXML private ConfigTabController defaultConfigViewController;
    @FXML private ConfigTabController userConfigViewController;

    private final ConfigRepository<KeybindConfig> defaultConfig;
    private final ConfigRepository<KeybindConfig> userConfig;

    public MainController(ConfigRepository<KeybindConfig> defaultConfig,
                          ConfigRepository<KeybindConfig> userConfig) {
        this.defaultConfig = defaultConfig;
        this.userConfig = userConfig;
    }

    @FXML
    private void initialize() {
        defaultConfigViewController.init(defaultConfig);
        userConfigViewController.init(userConfig);
    }
}
