package de.bsommerfeld.randomizer;

import de.bsommerfeld.randomizer.config.AppPreferences;
import de.bsommerfeld.randomizer.config.CrosshairService;
import de.bsommerfeld.randomizer.config.Cs2ConfigService;
import de.bsommerfeld.randomizer.gsi.GsiService;
import de.bsommerfeld.randomizer.steam.JnaWindowsRegistry;
import de.bsommerfeld.randomizer.steam.SteamLocator;
import de.bsommerfeld.randomizer.ui.CrosshairController;
import de.bsommerfeld.randomizer.ui.MainController;
import de.bsommerfeld.randomizer.ui.OverviewController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class RandomizerApp extends Application {

    private GsiService gsiService;

    @Override
    public void start(Stage stage) throws IOException {
        AppPreferences preferences = new AppPreferences();
        SteamLocator steamLocator = new SteamLocator(new JnaWindowsRegistry());
        Cs2ConfigService configService = new Cs2ConfigService(preferences, steamLocator);
        CrosshairService crosshairService = new CrosshairService(preferences, steamLocator);
        gsiService = new GsiService();

        FXMLLoader loader = new FXMLLoader(RandomizerApp.class.getResource("ui/main-view.fxml"));
        loader.setControllerFactory(controllerType -> {
            if (controllerType == OverviewController.class) {
                return new OverviewController(gsiService);
            }
            if (controllerType == CrosshairController.class) {
                return new CrosshairController(crosshairService);
            }
            return new MainController(configService, gsiService);
        });
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
