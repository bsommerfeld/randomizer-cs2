package de.bsommerfeld.randomizer.ui;

import de.bsommerfeld.randomizer.action.BoundKeys;
import de.bsommerfeld.randomizer.config.ConfigRepository;
import de.bsommerfeld.randomizer.config.keybinds.KeybindConfig;
import de.bsommerfeld.randomizer.ui.config.ConfigTabController;
import de.bsommerfeld.randomizer.ui.config.MergedConfigTabController;
import de.bsommerfeld.randomizer.ui.gsi.GsiTabController;
import de.bsommerfeld.randomizer.ui.randomizer.RandomizerTabController;
import de.bsommerfeld.randomizer.vdf.VdfObject;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.util.Duration;

import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.stream.Stream;

/**
 * The main window. Wires the included tabs to their configs and to each other, and shows the banner
 * above the tabs while CS2 is not running. Each tab's behavior lives in its own controller.
 */
public final class MainController {

    private static final Duration POLL_INTERVAL = Duration.seconds(3);

    @FXML private Label cs2Banner;
    @FXML private ConfigTabController defaultConfigViewController;
    @FXML private ConfigTabController userConfigViewController;
    @FXML private MergedConfigTabController mergedConfigViewController;
    @FXML private GsiTabController gsiViewController;
    @FXML private RandomizerTabController randomizerViewController;

    private final ConfigRepository defaultConfig;
    private final ConfigRepository userConfig;
    private final BooleanSupplier cs2Running;

    public MainController(ConfigRepository defaultConfig,
                          ConfigRepository userConfig,
                          BooleanSupplier cs2Running) {
        this.defaultConfig = defaultConfig;
        this.userConfig = userConfig;
        this.cs2Running = cs2Running;
    }

    @FXML
    private void initialize() {
        defaultConfigViewController.init(defaultConfig);
        userConfigViewController.init(userConfig);
        refreshMerged(); // for the case that the merged tab is the one open at startup
        randomizerViewController.init(gsiViewController::ensureStarted, this::readBoundKeys);
        refreshBanner();
        startBannerPolling();
    }

    private void startBannerPolling() {
        Timeline poll = new Timeline(new KeyFrame(POLL_INTERVAL, event -> refreshBanner()));
        poll.setCycleCount(Animation.INDEFINITE);
        poll.play();
    }

    /**
     * Rebuilds the merged view each time its tab is opened. A config can only change while its own
     * tab is open, so merging on selection always shows the current state of both.
     */
    @FXML
    private void onMergedSelected(Event event) {
        // A first tab is selected while the FXML is still loading, before the other tabs' controllers
        // are injected. initialize() does that first refresh instead.
        if (userConfigViewController != null && ((Tab) event.getSource()).isSelected()) {
            refreshMerged();
        }
    }

    private void refreshMerged() {
        mergedConfigViewController.show(
                defaultConfigViewController.current(), userConfigViewController.current());
    }

    /**
     * The keybinds as the files hold them right now, defaults first and custom on top like CS2 loads
     * them. Read anew instead of taken from the config tabs, so a bind changed in the game since the
     * app started counts. Empty when neither file could be loaded.
     */
    private Optional<BoundKeys> readBoundKeys() {
        List<VdfObject> configs = Stream.of(defaultConfig.loadOnStartup(), userConfig.loadOnStartup())
                .flatMap(Optional::stream)
                .map(KeybindConfig::model)
                .toList();
        return configs.isEmpty() ? Optional.empty() : Optional.of(BoundKeys.of(configs));
    }

    /** The banner's text is in main-view.fxml. */
    private void refreshBanner() {
        boolean show = !cs2Running.getAsBoolean();
        cs2Banner.setManaged(show);
        cs2Banner.setVisible(show);
    }
}
