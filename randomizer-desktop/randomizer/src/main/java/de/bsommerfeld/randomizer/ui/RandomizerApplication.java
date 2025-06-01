package de.bsommerfeld.randomizer.ui;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import de.bsommerfeld.model.action.sequence.ActionSequenceDispatcher;
import de.bsommerfeld.model.tracker.TimeTracker;
import de.bsommerfeld.randomizer.Main;
import de.bsommerfeld.randomizer.config.RandomizerConfig;
import de.bsommerfeld.randomizer.ui.view.ViewProvider;
import de.bsommerfeld.randomizer.ui.view.controller.RandomizerWindowController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RandomizerApplication extends Application {

  private static final int MIN_WIDTH = 704;
  private static final int MIN_HEIGHT = 536;

  @Override
  public void start(Stage stage) {
    log.debug("Starte Randomizer...");
    try {
      Thread.setDefaultUncaughtExceptionHandler(new UIUncaughtExceptionHandler());
      buildAndShowApplication(stage);
      log.debug("Hauptfenster angezeigt");
    } catch (Exception e) {
      log.error("Ein Fehler ist beim Starten der Anwendung aufgetreten", e);
    }
  }

  private void buildAndShowApplication(Stage stage) {
    ViewProvider viewProvider = Main.getInjector().getInstance(ViewProvider.class);
    TimeTracker timeTracker = Main.getInjector().getInstance(TimeTracker.class);
    RandomizerConfig randomizerConfig = Main.getInjector().getInstance(RandomizerConfig.class);
    log.debug("Lade Hauptfenster...");
    Parent root = viewProvider.requestView(RandomizerWindowController.class).parent();
    Scene scene = new Scene(root);
    setupStage(stage, scene);
    stage.setOnCloseRequest(
        _ -> {
          try {
            GlobalScreen.unregisterNativeHook();
          } catch (NativeHookException e) {
            throw new RuntimeException(e);
          }
          randomizerConfig.setTimeTracked(
              randomizerConfig.getTimeTracked() + timeTracker.getElapsedTime());
          randomizerConfig.save();
          ActionSequenceDispatcher.discardAllRunningActions();
          Platform.exit();
        });
    stage.show();
  }

  private void setupStage(Stage stage, Scene scene) {
    try {
      if (Main.isTestMode()) {
        stage.setTitle("Randomizer-CS2 - DEVELOPMENT");
      } else {
        stage.setTitle("Randomizer-CS2");
      }
    } catch (Exception e) {
      log.error("Fehler beim Laden der Version für Titel", e);
    }

    stage.getIcons().add(new Image("de/bsommerfeld/randomizer/images/randomizer.png"));
    scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
    stage.setMinWidth(MIN_WIDTH);
    stage.setMinHeight(MIN_HEIGHT);
    stage.setWidth(MIN_WIDTH);
    stage.setHeight(MIN_HEIGHT);
    stage.setScene(scene);
  }
}
