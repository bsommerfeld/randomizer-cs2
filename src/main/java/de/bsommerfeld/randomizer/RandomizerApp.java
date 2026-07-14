package de.bsommerfeld.randomizer;

import de.bsommerfeld.randomizer.config.AppPreferences;
import de.bsommerfeld.randomizer.config.Cs2ConfigService;
import de.bsommerfeld.randomizer.gsi.GsiService;
import de.bsommerfeld.randomizer.steam.JnaWindowsRegistry;
import de.bsommerfeld.randomizer.steam.SteamLocator;
import de.bsommerfeld.randomizer.ui.MainController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class RandomizerApp extends Application {

    private GsiService gsiService;

    @Override
    public void start(Stage stage) throws IOException {
        Cs2ConfigService configService = new Cs2ConfigService(
                new AppPreferences(),
                new SteamLocator(new JnaWindowsRegistry()));
        gsiService = new GsiService();

        FXMLLoader loader = new FXMLLoader(RandomizerApp.class.getResource("ui/main-view.fxml"));
        loader.setControllerFactory(controllerType -> new MainController(configService, gsiService));
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
