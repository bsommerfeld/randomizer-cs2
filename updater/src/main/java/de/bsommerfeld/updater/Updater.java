package de.bsommerfeld.updater;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Updater {

  public static final String UPDATER_VERSION_URL =
      "https://raw.githubusercontent.com/bsommerfeld/randomizer-cs2/stage/randomizer-desktop/randomizer-updater/src/main/resources/updater-version.txt";
  public static final String UPDATER_DOWNLOAD_URL =
      "https://github.com/bsommerfeld/randomizer-cs2/releases/download/latest/randomizer-updater.jar";
  public static final String RANDOMIZER_VERSION_URL =
      "https://raw.githubusercontent.com/bsommerfeld/randomizer-cs2/stage/randomizer-model/src/main/resources/model-version.txt";
  public static final String RANDOMIZER_DOWNLOAD_URL =
      "https://github.com/bsommerfeld/randomizer-cs2/releases/download/latest/randomizer.jar";

  private static final ExecutorService EXECUTOR_SERVICE = Executors.newCachedThreadPool();

  public static CompletionStage<Void> update(File target, String downloadUrl) {
    return update(target, downloadUrl, null);
  }

  public static CompletionStage<Void> update(
      File target, String downloadUrl, ProgressListener listener) {
    return CompletableFuture.runAsync(
            () -> {
              log("Starting update from URL: " + downloadUrl + " to target file: " + target);
              try {
                URL downloadFrom = new URL(downloadUrl);
                copyURLToFileWithProgress(downloadFrom, target, listener);
                log("Successfully updated from URL: " + downloadUrl + " to target file: " + target);
              } catch (IOException e) {
                throw new IllegalStateException(
                    "Error updating from URL: " + downloadUrl + " to target file: " + target, e);
              }
            },
            EXECUTOR_SERVICE)
        .exceptionally(
            ex -> {
              log("Exception during update: " + ex.getMessage());
              throw new IllegalStateException(ex);
            });
  }

  private static void copyURLToFileWithProgress(
      URL source, File destination, ProgressListener listener) throws IOException {
    HttpURLConnection connection = (HttpURLConnection) source.openConnection();
    connection.setRequestMethod("GET");

    try (InputStream inputStream = connection.getInputStream()) {
      long totalBytes = connection.getContentLengthLong();

      File parentDir = destination.getParentFile();
      if (parentDir != null && !parentDir.exists()) {
        if (!parentDir.mkdirs()) {
          throw new IOException("Failed to create directory: " + parentDir.getAbsolutePath());
        }
      }

      try (FileOutputStream outputStream = new FileOutputStream(destination)) {
        byte[] buffer = new byte[1024];
        long bytesRead = 0;
        int n;
        while ((n = inputStream.read(buffer)) != -1) {
          outputStream.write(buffer, 0, n);
          bytesRead += n;
          if (listener != null) {
            listener.onProgress(bytesRead, totalBytes);
          }
        }
        log(
            "Data successfully copied from URL: "
                + source
                + " to destination file: "
                + destination);
      }
    } finally {
      connection.disconnect();
    }
  }

  public enum FileType {
    UPDATER("updater-"),
    RANDOMIZER("model-");

    private final String prefix;

    FileType(String prefix) {
      this.prefix = prefix;
    }

    public String getPrefix() {
      return prefix;
    }
  }

  public static CompletionStage<Boolean> isUpdateAvailable(
      File toUpdate, String versionUrl, FileType fileType) {
    return CompletableFuture.supplyAsync(
            () -> {
              log("Checking for update: File = " + toUpdate + ", Version URL = " + versionUrl);

              if (!toUpdate.exists()) {
                log("Update available: Target file does not exist: " + toUpdate);
                return true;
              }

              String version =
                  getVersion(toUpdate, fileType).toCompletableFuture().join(); // get the version
              log("Checking update for version=" + version + " of file=" + toUpdate);

              boolean updateAvailable =
                  isUpdateAvailable(version, versionUrl).toCompletableFuture().join();
              log(
                  "Update availability checked: current version = "
                      + version
                      + ", Update available = "
                      + updateAvailable);
              return updateAvailable;
            },
            EXECUTOR_SERVICE)
        .exceptionally(
            ex -> {
              log("Exception during update check: " + ex.getMessage());
              throw new IllegalStateException(ex);
            });
  }

  public static CompletionStage<String> getVersion(File file, FileType fileType) {
    return CompletableFuture.supplyAsync(
            () -> {
              if (!file.exists()) {
                log("File not found: " + file);
                return "NOT FOUND";
              }

              String versionFile = fileType.getPrefix() + "version.txt";
              try (ZipFile zipFile = new ZipFile(file)) {
                ZipEntry versionEntry = zipFile.getEntry(versionFile);
                if (versionEntry == null) {
                  return "NOT FOUND";
                }

                return readLineFromInputStream(zipFile.getInputStream(versionEntry));
              } catch (IOException e) {
                throw new IllegalStateException("Error reading version from file: " + file, e);
              }
            },
            EXECUTOR_SERVICE)
        .exceptionally(
            ex -> {
              log("Exception during version retrieval: " + ex.getMessage());
              return "NOT FOUND";
            });
  }

  public static CompletionStage<String> getLatestVersion(String versionUrl) {
    return CompletableFuture.supplyAsync(
            () -> {
              log("Retrieving latest version from URL: " + versionUrl);
              try {
                URL url = new URL(versionUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                try (BufferedReader in =
                    new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                  String latestVersion = in.readLine();
                  log("Successfully retrieved: latest version = " + latestVersion);
                  return latestVersion;
                } finally {
                  connection.disconnect();
                }
              } catch (IOException e) {
                throw new IllegalStateException(
                    "Error retrieving latest version from URL: " + versionUrl, e);
              }
            },
            EXECUTOR_SERVICE)
        .exceptionally(
            ex -> {
              log("Exception during latest version retrieval: " + ex.getMessage());
              return "NOT FOUND";
            });
  }

  public static CompletionStage<Boolean> isUpdateAvailable(String version, String versionUrl) {
    return CompletableFuture.supplyAsync(
            () -> {
              log(
                  "Checking for update with current version: "
                      + version
                      + " using version URL: "
                      + versionUrl);

              UpdateChecker updateChecker = new UpdateChecker(versionUrl);
              updateChecker.checkUpdate(version);

              boolean updateAvailable = updateChecker.isUpdateAvailable();
              log(
                  "Update check completed: Version URL = "
                      + versionUrl
                      + ", Update available = "
                      + updateAvailable);
              return updateAvailable;
            },
            EXECUTOR_SERVICE)
        .exceptionally(
            ex -> {
              log("Exception during update availability check: " + ex.getMessage());
              if (ex instanceof RuntimeException) {
                throw (RuntimeException) ex;
              }
              throw new RuntimeException(ex);
            });
  }

  public static void close() {
      EXECUTOR_SERVICE.shutdown();
  }

  private static String readLineFromInputStream(InputStream inputStream) throws IOException {
    try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
      return bufferedReader.readLine();
    }
  }

  private static void log(String message) {
    System.out.println(message); // TODO: add proper logging
  }
}
