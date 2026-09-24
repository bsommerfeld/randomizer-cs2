package de.bsommerfeld.randomizer.ui;

import de.bsommerfeld.randomizer.action.ActionRunner;
import de.bsommerfeld.randomizer.action.FireGate;
import de.bsommerfeld.randomizer.config.AppPreferences;
import de.bsommerfeld.randomizer.config.ConfigRepository;
import de.bsommerfeld.randomizer.config.keybinds.KeybindConfig;
import de.bsommerfeld.randomizer.gsi.GsiService;
import de.bsommerfeld.randomizer.steam.SteamLocator;
import de.bsommerfeld.randomizer.ui.config.ConfigTabController;
import de.bsommerfeld.randomizer.ui.config.MergedConfigTabController;
import de.bsommerfeld.randomizer.ui.gsi.GsiTabController;
import de.bsommerfeld.randomizer.ui.overview.OverviewController;
import de.bsommerfeld.randomizer.ui.randomizer.RandomizerTabController;
import javafx.util.Callback;

import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/** Builds each controller the FXML loader asks for, with its dependencies. */
public final class ControllerFactory {

    private ControllerFactory() {
    }

    public static Callback<Class<?>, Object> create(AppPreferences preferences,
                                                    SteamLocator steamLocator,
                                                    GsiService gsiService,
                                                    BooleanSupplier cs2Running,
                                                    ActionRunner actionRunner,
                                                    FireGate fireGate) {
        ConfigRepository defaultConfig = new ConfigRepository(KeybindConfig.defaults(steamLocator), preferences);
        ConfigRepository userConfig = new ConfigRepository(KeybindConfig.custom(steamLocator), preferences);
        Map<Class<?>, Supplier<Object>> registry = Map.of(
                MainController.class,
                () -> new MainController(defaultConfig, userConfig, cs2Running),
                ConfigTabController.class, ConfigTabController::new,
                MergedConfigTabController.class, MergedConfigTabController::new,
                GsiTabController.class, () -> new GsiTabController(gsiService, cs2Running),
                OverviewController.class, () -> new OverviewController(gsiService),
                RandomizerTabController.class,
                () -> new RandomizerTabController(actionRunner, fireGate, preferences));
        return type -> {
            Supplier<Object> supplier = registry.get(type);
            if (supplier == null) {
                throw new IllegalStateException("No controller registered for " + type.getName());
            }
            return supplier.get(); 
        };
    }
}
