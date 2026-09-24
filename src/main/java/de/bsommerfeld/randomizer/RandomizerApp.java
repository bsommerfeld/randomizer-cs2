package de.bsommerfeld.randomizer;

import de.bsommerfeld.randomizer.action.ActionRunner;
import de.bsommerfeld.randomizer.action.FireGate;
import de.bsommerfeld.randomizer.config.AppPreferences;
import de.bsommerfeld.randomizer.game.ProcessGameDetector;
import de.bsommerfeld.randomizer.gsi.GsiService;
import de.bsommerfeld.randomizer.input.JnaGameInput;
import de.bsommerfeld.randomizer.input.RawInputWatcher;
import de.bsommerfeld.randomizer.steam.JnaWindowsRegistry;
import de.bsommerfeld.randomizer.steam.SteamLocator;
import de.bsommerfeld.randomizer.ui.ControllerFactory;
import javafx.application.Application;
import javafx.application.ColorScheme;
import javafx.css.PseudoClass;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.HeaderBar;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;

/** JavaFX entry point and composition root: builds the object graph and shows the main window. */
public class RandomizerApp extends Application {

    private static final PseudoClass WINDOW_FOCUSED = PseudoClass.getPseudoClass("window-focused");

    private GsiService gsiService;
    private ActionRunner actionRunner;

    @Override
    public void start(Stage stage) throws IOException {
        gsiService = new GsiService();
        JnaGameInput gameInput = new JnaGameInput();
        FireGate fireGate = new FireGate(gsiService, gameInput::isCs2Foreground);
        actionRunner = new ActionRunner(gameInput, fireGate::isOpen,
                () -> gsiService.currentGameState().player, new RawInputWatcher(gameInput::isCs2Foreground));
        FXMLLoader loader = new FXMLLoader(RandomizerApp.class.getResource("ui/main-view.fxml"));
        loader.setControllerFactory(ControllerFactory.create(new AppPreferences(),
                new SteamLocator(new JnaWindowsRegistry()), gsiService, ProcessGameDetector::isCs2Running,
                actionRunner, fireGate));
        Parent root = loader.load();
        // The fill is -rz-ground from app.css. It shows at the edges while the window resizes, white would flash there.
        Scene scene = new Scene(root, Color.web("#1f242b"));
        scene.getStylesheets().add(RandomizerApp.class.getResource("ui/app.css").toExternalForm());
        // The theme is dark no matter what Windows is set to, so the window buttons must be the light-on-dark ones.
        scene.getPreferences().setColorScheme(ColorScheme.DARK);

        stage.focusedProperty().addListener((obs, was, focused) -> root.pseudoClassStateChanged(WINDOW_FOCUSED, focused));
        stage.initStyle(StageStyle.EXTENDED);
        // The window buttons stay at JavaFX's 29 px unless told to fill the header bar, whose height main-view.fxml sets.
        HeaderBar.setSystemButtonHeight(stage, ((HeaderBar) root.lookup(".header-bar")).getMinHeight());
        stage.setTitle("Randomizer CS2");
        // Below this the scoreboard has no room left next to the 260 px player card.
        stage.setMinWidth(720);
        stage.setMinHeight(480);
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        if (actionRunner != null) {
            actionRunner.stop(); // first, it releases a key it may hold and ends the raw input watching
        }
        if (gsiService != null) {
            gsiService.close();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
