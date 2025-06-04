package de.bsommerfeld.randomizer.ui;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import de.bsommerfeld.model.action.spi.ActionSequenceDispatcher;
import de.bsommerfeld.model.action.spi.ActionSequenceExecutor;
import de.bsommerfeld.randomizer.Main;
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
  private static final int MAX_WIDTH = 1280;
  private static final int MAX_HEIGHT = 720;

  @Override
  public void start(Stage stage) {
    log.debug("Starting Randomizer...");
    try {
      Thread.setDefaultUncaughtExceptionHandler(new UIUncaughtExceptionHandler());
      buildAndShowApplication(stage);
      log.debug("Main window displayed");
    } catch (Exception e) {
      log.error("An error occurred while starting the application", e);
    }
  }

  private void buildAndShowApplication(Stage stage) {
    ViewProvider viewProvider = Main.getInjector().getInstance(ViewProvider.class);
    ActionSequenceDispatcher actionSequenceDispatcher =
        Main.getInjector().getInstance(ActionSequenceDispatcher.class);
    ActionSequenceExecutor actionSequenceExecutor =
        Main.getInjector().getInstance(ActionSequenceExecutor.class);
    log.debug("Loading main window...");
    buildApplication(stage, viewProvider, actionSequenceDispatcher, actionSequenceExecutor);
  }

  private void buildApplication(
      Stage stage,
      ViewProvider viewProvider,
      ActionSequenceDispatcher actionSequenceDispatcher,
      ActionSequenceExecutor actionSequenceExecutor) {
    Parent root = viewProvider.requestView(RandomizerWindowController.class).parent();
    Scene scene = new Scene(root);
    setupStage(stage, scene);
    stage.setOnCloseRequest(
        _ -> {
          try {
            log.info("Unregistering native hook...");
            GlobalScreen.unregisterNativeHook();
          } catch (NativeHookException e) {
            log.error("Failed to unregister native hook", e);
            // We don't want to throw anything on close request
          } finally {
            log.info("Closing application, stopping executor and discarding running actions...");
            actionSequenceDispatcher.discardAllRunningActions();
            actionSequenceExecutor.stop();
            Platform.exit();
          }
        });
    stage.show();
  }

  private void setupStage(Stage stage, Scene scene) {
    stage.setTitle("Randomizer " + Main.getRandomizerVersion());
    stage.getIcons().add(new Image("de/bsommerfeld/randomizer/images/randomizer.png"));
    scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
    stage.setMinWidth(MIN_WIDTH);
    stage.setMinHeight(MIN_HEIGHT);
    stage.setWidth(MIN_WIDTH);
    stage.setHeight(MIN_HEIGHT);
    stage.setMaxWidth(MAX_WIDTH);
    stage.setMaxHeight(MAX_HEIGHT);
    stage.setScene(scene);
  }
}
