package de.bsommerfeld.randomizer.bootstrap;

import de.bsommerfeld.model.ApplicationContext;
import de.bsommerfeld.updater.Updater;
import de.bsommerfeld.updater.util.JarFileUtil;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javafx.application.Platform;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RandomizerUpdater {

  public void runUpdaterIfNeeded() {
    log.info("Setting up appdata directory...");
    File updater = getUpdater();
    startUpdaterIfNecessary(updater.getAbsolutePath());
  }

  public CompletionStage<Boolean> isRandomizerUpdateAvailable() {
    try {
      return Updater.isUpdateAvailable(
          JarFileUtil.getJarFile(), Updater.RANDOMIZER_VERSION_URL, Updater.FileType.RANDOMIZER);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  public File getUpdater() {
    File updaterJar = new File(ApplicationContext.getAppdataFolder(), "randomizer-updater.jar");
    if (!updaterJar.exists()) {
      installUpdater();
    }
    return updaterJar;
  }

  public CompletionStage<String> getRandomizerVersion() {
    try {
      return Updater.getVersion(JarFileUtil.getJarFile(), Updater.FileType.RANDOMIZER);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  public File installUpdater() {
    log.info("Installing Updater...");
    File updaterJar = new File(ApplicationContext.getAppdataFolder(), "randomizer-updater.jar");

    try {
      if (!updaterJar.exists()) {
        updaterJar.getParentFile().mkdirs();
        updaterJar.createNewFile();
      }
    } catch (IOException e) {
      log.error("Error creating Updater file", e);
      throw new IllegalStateException(e);
    }

    Updater.isUpdateAvailable(updaterJar, Updater.UPDATER_VERSION_URL, Updater.FileType.UPDATER)
        .thenCompose(
            updateAvailable -> {
              if (Boolean.TRUE.equals(updateAvailable)) {
                log.info("Updating updater...");
                return Updater.update(updaterJar, Updater.UPDATER_DOWNLOAD_URL);
              }
              return CompletableFuture.completedFuture(null);
            })
        .exceptionally(
            e -> {
              log.error("Error updating updater", e);
              throw new IllegalStateException(e);
            })
        .toCompletableFuture()
        .join();
    return updaterJar;
  }

  private void startUpdaterIfNecessary(String updaterPath) {
    log.info("Checking for Randomizer updates...");
    try {
      File jarPath = JarFileUtil.getJarFile();

      isRandomizerUpdateAvailable()
          .thenAccept(
              updateAvailable -> {
                if (Boolean.TRUE.equals(updateAvailable)) {
                  log.info("Randomizer update available. Starting Updater...");
                  try {
                    ProcessBuilder processBuilder =
                        new ProcessBuilder(
                            "java",
                            "-jar",
                            updaterPath,
                            "-randomizerLocation=" + jarPath.getAbsolutePath());
                    processBuilder.inheritIO();
                    processBuilder.start();
                    Platform.runLater(Platform::exit);
                  } catch (IOException e) {
                    log.error("Failed to start updater process.", e);
                    throw new IllegalStateException("Failed to start updater", e);
                  }
                } else {
                  log.info("No Randomizer update available.");
                }
              })
          .exceptionally(
              ex -> {
                log.error("Error during update check.", ex);
                return null;
              });

    } catch (IOException e) {
      log.error("Failed to get current JAR path.", e);
      throw new IllegalStateException("Failed to determine current JAR path", e);
    }
  }
}
