package de.bsommerfeld.randomizer.ui;

import de.bsommerfeld.model.ApplicationContext;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import javafx.scene.control.Alert;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UIUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {

  private static final long DIALOG_COOLDOWN_MS = 5000; // 5 seconds

  private static final String GITHUB_ISSUES_URL =
      "https://github.com/Metaphoriker/randomizer-cs2/issues";

  private long lastDialogShownTime = 0;

  @Override
  public void uncaughtException(Thread t, Throwable e) {
    log.error("Unexpected error", e);

    long currentTime = System.currentTimeMillis();
    if (currentTime - lastDialogShownTime > DIALOG_COOLDOWN_MS) {
      lastDialogShownTime = currentTime;

      try {
        Alert alert = createStyledAlert();
        VBox content = createContent();
        alert.getDialogPane().setContent(content);
        alert.showAndWait();
      } catch (Exception ex) {
        log.error(
            "An unexpected error occurred, and the error dialog could not be displayed. Secondary exception: ",
            ex);
      }
    } else {
      log.error("Suppressed error dialog due to cooldown.", e);
    }
  }

  private Alert createStyledAlert() {
    Alert alert = new Alert(Alert.AlertType.ERROR);
    alert
        .getDialogPane()
        .getStylesheets()
        .add(getClass().getResource("alert-style.css").toExternalForm());
    alert.setTitle("User Error Dialog");
    alert.setHeaderText("Something happened");
    return alert;
  }

  private VBox createContent() {
    Hyperlink githubLink =
        createHyperlink(
            "Click here to get to the Issue section on GitHub.", () -> openUrl(GITHUB_ISSUES_URL));
    Hyperlink logsLink =
        createHyperlink("Click here to direct to your logs folder.", this::openLogsFolder);

    VBox content = new VBox(10);
    content
        .getChildren()
        .addAll(
            new Label("Please report this error to us on GitHub."),
            new Label("The following information will be helpful in debugging this issue"),
            githubLink,
            logsLink);
    return content;
  }

  private Hyperlink createHyperlink(String text, Runnable action) {
    Hyperlink hyperlink = new Hyperlink(text);
    hyperlink.setOnAction(_ -> action.run());
    return hyperlink;
  }

  private void openUrl(String url) {
    try {
      Desktop.getDesktop().browse(new URI(url));
    } catch (IOException | URISyntaxException e) {
      log.error("Could not open URL: {}", url, e);
      throw new IllegalStateException(e);
    }
  }

  private void openLogsFolder() {
    try {
      File logsFolder = ApplicationContext.getAppdataLogsFolder();
      if (logsFolder.exists() && logsFolder.isDirectory()) {
        Desktop.getDesktop().open(logsFolder);
      } else {
        log.error("Log folder does not exist: {}", logsFolder.getAbsolutePath());
        throw new IllegalStateException(
            "Log folder does not exist: " + logsFolder.getAbsolutePath());
      }
    } catch (IOException e) {
      log.error("Could not open logs folder", e);
      throw new IllegalStateException(e);
    }
  }
}
