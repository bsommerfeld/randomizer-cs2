package de.bsommerfeld.updater;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class UpdateChecker {

  private final String versionUrl;

  private boolean updateAvailable = false; // Initialize to false

  public UpdateChecker(String versionUrl) {
    this.versionUrl = versionUrl;
  }

  /**
   * Checks for the availability of an update by comparing the current version with the latest
   * version available online. Logs the process and the result of the update check.
   *
   * @param currentVersion The current version of the software to be checked against the latest
   *     version available online.
   */
  public void checkUpdate(String currentVersion) {
    log(
        "Starte Update-Check: aktuelle Version = "
            + currentVersion
            + ", Versions-URL = "
            + versionUrl);

    HttpURLConnection connection = null;
    try {
      URL url = new URL(versionUrl);
      connection = (HttpURLConnection) url.openConnection();
      connection.setRequestMethod("GET"); // Set request method explicitly
      connection.connect();

      String latestVersion = readLineFromInputStream(connection.getInputStream());

      if ("UNGÜLTIG".equals(latestVersion) || "NOT FOUND".equals(currentVersion)) {
        updateAvailable = true;
      } else if (!currentVersion.equals(latestVersion)) {
        try {
          Version current = new Version(currentVersion);
          Version latest = new Version(latestVersion);
          updateAvailable = latest.compareTo(current) > 0;
        } catch (IllegalArgumentException e) {
          log("Konnte versionen nicht parsen. " + e.getMessage());
          updateAvailable = true;
        }
      }

      log(
          "Update-Check abgeschlossen: neueste Version = "
              + latestVersion
              + ", Update verfügbar = "
              + updateAvailable);
    } catch (IOException e) {
      log("Fehler beim Prüfen auf Update: " + e.getMessage()); // More specific message
      updateAvailable = true; // Consider an update available on error
    } finally {
      if (connection != null) {
        connection.disconnect();
        log("Verbindung geschlossen");
      }
    }
  }

  public boolean isUpdateAvailable() {
    return updateAvailable;
  }

  private String readLineFromInputStream(InputStream inputStream) {
    try (BufferedReader bufferedReader =
        new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
      return bufferedReader.readLine();
    } catch (IOException e) {
      log("Fehler beim Lesen der Zeile aus dem InputStream: " + e.getMessage());
      return "UNGÜLTIG";
    }
  }

  // Simple logging method
  private static void log(String message) {
    System.out.println(message);
  }
}
