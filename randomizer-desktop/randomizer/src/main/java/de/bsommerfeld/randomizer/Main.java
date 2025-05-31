package de.bsommerfeld.randomizer;

import com.google.inject.Guice;
import com.google.inject.Injector;
import de.bsommerfeld.model.ApplicationContext;
import de.bsommerfeld.model.ModelModule;
import de.bsommerfeld.randomizer.bootstrap.RandomizerBootstrap;
import de.bsommerfeld.randomizer.bootstrap.RandomizerModule;
import de.bsommerfeld.randomizer.ui.RandomizerApplication;
import javafx.application.Application;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Main entry point for the Randomizer-CS2 application.
 * This class initializes the application context, dependency injection,
 * and launches the JavaFX user interface.
 */
@Slf4j
public class Main {

  /** Command line flag to enable test mode */
  private static final String TEST_MODE_FLAG = "-testMode=";

  /** Guice injector for dependency injection */
  @Getter private static final Injector injector = initializeInjector();

  /** Flag indicating whether the application is running in test mode */
  @Getter private static boolean testMode = false;

  /**
   * Static initializer block to create necessary application directories
   */
  static {
    ApplicationContext.getAppdataFolder().mkdirs();
    ApplicationContext.getAppdataLibsFolder().mkdirs();
  }

  /**
   * Main entry point for the application.
   * Sets up the environment, initializes the application, and launches the UI.
   *
   * @param args Command line arguments
   */
  public static void main(String[] args) {
    System.setProperty(
        "jnativehook.lib.path", ApplicationContext.getAppdataLibsFolder().getAbsolutePath());

    verifyTestMode(args);
    initializeApplication();
    launchApplication(args);
  }

  /**
   * Launches the JavaFX application.
   *
   * @param args Command line arguments to pass to the JavaFX application
   */
  private static void launchApplication(String[] args) {
    log.debug("Starting JavaFX application...");
    Application.launch(RandomizerApplication.class, args);
  }

  /**
   * Initializes the application by bootstrapping required components.
   */
  private static void initializeApplication() {
    RandomizerBootstrap randomizerBootstrap = injector.getInstance(RandomizerBootstrap.class);
    randomizerBootstrap.initializeApplication();
  }

  /**
   * Checks command line arguments for test mode flag and sets the mode accordingly.
   *
   * @param args Command line arguments to check
   */
  private static void verifyTestMode(String[] args) {
    testMode = hasTestModeFlag(args);
    if (testMode) {
      log.debug("Application started in test mode");
    }
  }

  /**
   * Checks if the test mode flag is present in the command line arguments.
   *
   * @param args Command line arguments to check
   * @return true if test mode is enabled, false otherwise
   */
  private static boolean hasTestModeFlag(String[] args) {
    for (String arg : args) {
      if (arg.startsWith(TEST_MODE_FLAG)) {
        return Boolean.parseBoolean(arg.substring(TEST_MODE_FLAG.length()));
      }
    }
    return false;
  }

  /**
   * Initializes the Guice injector with required modules.
   *
   * @return Configured Guice injector
   */
  private static Injector initializeInjector() {
    log.debug("Initializing Guice Injector");
    return Guice.createInjector(new ModelModule(), new RandomizerModule());
  }
}
