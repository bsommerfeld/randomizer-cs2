package de.bsommerfeld.randomizer.ui;

import de.bsommerfeld.randomizer.config.AppPreferences;
import de.bsommerfeld.randomizer.config.ConfigRepository;
import de.bsommerfeld.randomizer.config.ConfigSaver;
import de.bsommerfeld.randomizer.config.ConfigVerifier;
import de.bsommerfeld.randomizer.config.crosshair.Crosshair;
import de.bsommerfeld.randomizer.config.crosshair.CrosshairRandomizer;
import de.bsommerfeld.randomizer.config.crosshair.CrosshairStandard;
import de.bsommerfeld.randomizer.config.keybinds.KeybindConfig;
import de.bsommerfeld.randomizer.exec.ExecApplier;
import de.bsommerfeld.randomizer.gsi.GsiService;
import de.bsommerfeld.randomizer.ui.config.ConfigTabController;
import de.bsommerfeld.randomizer.ui.crosshair.CrosshairController;
import de.bsommerfeld.randomizer.ui.gsi.GsiTabController;
import de.bsommerfeld.randomizer.ui.overview.OverviewController;
import javafx.util.Callback;

import java.util.Map;
import java.util.function.Supplier;

/**
 * The controller registry for the FXML loader: maps each controller class to its construction
 * with the right dependencies. A new tab is wired by adding one registry entry - no existing
 * wiring changes (open/closed principle).
 */
public final class ControllerFactory {

    private ControllerFactory() {
    }

    public static Callback<Class<?>, Object> create(ConfigRepository<KeybindConfig> defaultConfig,
                                                    ConfigRepository<KeybindConfig> userConfig,
                                                    ConfigRepository<Crosshair> crosshairConfig,
                                                    CrosshairStandard crosshairStandard,
                                                    ExecApplier execApplier,
                                                    AppPreferences preferences,
                                                    CrosshairRandomizer crosshairRandomizer,
                                                    ConfigSaver configSaver,
                                                    ConfigVerifier configVerifier,
                                                    GsiService gsiService) {
        Map<Class<?>, Supplier<Object>> registry = Map.of(
                MainController.class, () -> new MainController(defaultConfig, userConfig),
                ConfigTabController.class, ConfigTabController::new,
                GsiTabController.class, () -> new GsiTabController(gsiService),
                OverviewController.class, () -> new OverviewController(gsiService),
                CrosshairController.class,
                () -> new CrosshairController(crosshairConfig, crosshairStandard, execApplier,
                        preferences, crosshairRandomizer, configSaver, configVerifier));
        return type -> {
            Supplier<Object> supplier = registry.get(type);
            if (supplier == null) {
                throw new IllegalStateException("Kein Controller registriert für " + type.getName());
            }
            return supplier.get();
        };
    }
}
