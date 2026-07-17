package de.bsommerfeld.randomizer;

import de.bsommerfeld.randomizer.config.AppPreferences;
import de.bsommerfeld.randomizer.config.ConfigRepository;
import de.bsommerfeld.randomizer.config.crosshair.Crosshair;
import de.bsommerfeld.randomizer.config.crosshair.CrosshairBackup;
import de.bsommerfeld.randomizer.config.crosshair.CrosshairConfigParser;
import de.bsommerfeld.randomizer.config.crosshair.CrosshairSource;
import de.bsommerfeld.randomizer.config.crosshair.CrosshairStandard;
import de.bsommerfeld.randomizer.config.keybinds.ConfigKind;
import de.bsommerfeld.randomizer.config.keybinds.KeybindConfig;
import de.bsommerfeld.randomizer.config.keybinds.KeybindConfigParser;
import de.bsommerfeld.randomizer.exec.ExecApplier;
import de.bsommerfeld.randomizer.exec.ExecConfig;
import de.bsommerfeld.randomizer.exec.JnaCs2Window;
import de.bsommerfeld.randomizer.gsi.GsiService;
import de.bsommerfeld.randomizer.steam.JnaWindowsRegistry;
import de.bsommerfeld.randomizer.steam.SteamLocator;
import de.bsommerfeld.randomizer.ui.ControllerFactory;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/** JavaFX entry point and composition root: builds the object graph and shows the main window. */
public class RandomizerApp extends Application {

    private GsiService gsiService;

    @Override
    public void start(Stage stage) throws IOException {
        AppPreferences preferences = new AppPreferences();
        SteamLocator steamLocator = new SteamLocator(new JnaWindowsRegistry());
        KeybindConfigParser keybindParser = new KeybindConfigParser();
        ConfigRepository<KeybindConfig> defaultConfig =
                new ConfigRepository<>(ConfigKind.DEFAULT, preferences, steamLocator, keybindParser);
        ConfigRepository<KeybindConfig> userConfig =
                new ConfigRepository<>(ConfigKind.USER, preferences, steamLocator, keybindParser);
        ConfigRepository<Crosshair> crosshairConfig = new ConfigRepository<>(
                CrosshairSource.INSTANCE, preferences, steamLocator, new CrosshairConfigParser());
        CrosshairStandard crosshairStandard = new CrosshairStandard(new CrosshairBackup());
        ExecApplier execApplier = new ExecApplier(
                new ExecConfig(steamLocator::findCs2CfgFolder), new JnaCs2Window(),
                () -> preferences.getString(ExecApplier.KEY_PREFERENCE, ExecApplier.DEFAULT_KEY));
        gsiService = new GsiService();

        FXMLLoader loader = new FXMLLoader(RandomizerApp.class.getResource("ui/main-view.fxml"));
        loader.setControllerFactory(ControllerFactory.create(
                defaultConfig, userConfig, crosshairConfig, crosshairStandard, execApplier,
                preferences, gsiService));
        Scene scene = new Scene(loader.load());

        stage.setTitle("Randomizer CS2");
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        if (gsiService != null) {
            gsiService.close();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
