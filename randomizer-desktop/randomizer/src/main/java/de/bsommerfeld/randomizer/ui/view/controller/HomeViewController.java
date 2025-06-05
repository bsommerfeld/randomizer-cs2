package de.bsommerfeld.randomizer.ui.view.controller;

import com.google.inject.Inject;
import de.bsommerfeld.randomizer.ui.RandomizerApplication;
import de.bsommerfeld.randomizer.ui.view.View;
import de.bsommerfeld.randomizer.ui.view.viewmodel.HomeViewModel;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@View
public class HomeViewController {

  private static final Duration ANIMATION_DURATION = Duration.millis(400);

  private final HomeViewModel homeViewModel;
  // Release data storage
  private final Map<String, ReleaseData> releasesData = new HashMap<>();
  @FXML private AnchorPane rootPane;
  @FXML private VBox mainContent;
  @FXML private Label starsLabel;
  @FXML private VBox releasesView;
  @FXML private VBox releasesListContent;
  @FXML private Label changelogTitle;
  @FXML private Label changelogText;
  private boolean isReleasesVisible = false;
  private String selectedReleaseVersion = null;

  @Inject
  public HomeViewController(HomeViewModel homeViewModel) {
    this.homeViewModel = homeViewModel;
  }

  @FXML
  private void initialize() {
    setupResponsiveLayout();
    setupReleasesData();
    setupGitHubDetailsBindings();
    populateReleasesList();
  }

  @FXML
  private void onRepositoryDetailsClick(MouseEvent event) {
    onGitHubOpen(event);
  }

  private void setupGitHubDetailsBindings() {
    starsLabel.textProperty().bind(homeViewModel.getStarsProperty().asString());
  }

  public void updateView() {
    homeViewModel.updateRepositoryDetails();
  }

  private void setupResponsiveLayout() {
    // Initial positioning of releases view below visible area
    rootPane
        .heightProperty()
        .addListener(
            (observable, oldValue, newValue) -> {
              if (!isReleasesVisible) {
                // Keep releases view positioned below the visible area
                double height = newValue.doubleValue();
                AnchorPane.setTopAnchor(releasesView, height);
                AnchorPane.setBottomAnchor(releasesView, -height);
              }
            });

    // Set initial position when the scene is ready
    Platform.runLater(
        () -> {
          if (rootPane.getHeight() > 0) {
            double height = rootPane.getHeight();
            AnchorPane.setTopAnchor(releasesView, height);
            AnchorPane.setBottomAnchor(releasesView, -height);
          }
        });
  }

  private void setupReleasesData() {
    // Sample release data - später aus ViewModel oder Service
    releasesData.put(
        "v1.2.0",
        new ReleaseData(
            "v1.2.0",
            "03.06.2025",
            "🚀 Neue Features:\n"
                + "• Verbesserte Performance um 40%\n"
                + "• Neue Action Sequence Templates\n"
                + "• Enhanced CS2 Integration\n"
                + "• Erweiterte Hotkey-Unterstützung\n\n"
                + "🐛 Bug-Fixes:\n"
                + "• Behoben: Crash beim Laden großer Konfigurationen\n"
                + "• Behoben: Memory Leak in Action Dispatcher\n"
                + "• Behoben: UI-Freezing bei langen Sequenzen\n\n"
                + "⚠️ Bekannte Probleme:\n"
                + "• Gelegentliche Verbindungsprobleme bei sehr langsamer Internetverbindung\n"
                + "• Beta-Feature 'Smart Randomization' noch experimentell"));

    releasesData.put(
        "v1.1.0",
        new ReleaseData(
            "v1.1.0",
            "20.05.2025",
            "✨ Highlights:\n"
                + "• Action Sequence Builder komplett überarbeitet\n"
                + "• Neue intuitive Benutzeroberfläche\n"
                + "• Unterstützung für komplexe Timing-Patterns\n\n"
                + "🔧 Verbesserungen:\n"
                + "• Stabilität der Action-Ausführung erhöht\n"
                + "• Bessere Fehlerbehandlung\n"
                + "• Optimierte Speicherverwaltung\n\n"
                + "🎯 Compatibility:\n"
                + "• CS2 Version 1.39+ erforderlich\n"
                + "• Windows 10+ und macOS 11+ unterstützt\n"
                + "• Java 21+ vorausgesetzt\n\n"
                + "📦 Migration:\n"
                + "Backup Ihrer Konfigurationen vor dem Update empfohlen!"));

    releasesData.put(
        "v1.0.0",
        new ReleaseData(
            "v1.0.0",
            "01.05.2025",
            "🎉 Erste stabile Version!\n\n"
                + "🌟 Hauptfeatures:\n"
                + "• Vollständige CS2 Integration\n"
                + "• Drag & Drop Action Sequence Builder\n"
                + "• Erweiterte Randomizer-Funktionen\n"
                + "• Benutzerfreundliche Oberfläche\n"
                + "• Umfangreiche Konfigurationsmöglichkeiten\n\n"
                + "⚙️ System-Anforderungen:\n"
                + "• Windows 10+ oder macOS 11+\n"
                + "• Java 21+\n"
                + "• CS2 installiert und konfiguriert\n"
                + "• Mindestens 2GB RAM verfügbar\n\n"
                + "🎮 Erste Schritte:\n"
                + "1. CS2 Config-Pfad in Einstellungen setzen\n"
                + "2. Action Sequences konfigurieren\n"
                + "3. Randomizer starten und genießen!"));
  }

  private void populateReleasesList() {
    releasesListContent.getChildren().clear();

    releasesData.entrySet().stream()
        .sorted((e1, e2) -> e2.getKey().compareTo(e1.getKey())) // Sort by version desc
        .forEach(
            entry -> {
              ReleaseData release = entry.getValue();
              HBox releaseEntry = createReleaseEntry(release);
              releasesListContent.getChildren().add(releaseEntry);
            });
  }

  private HBox createReleaseEntry(ReleaseData release) {
    HBox entry = new HBox();
    entry.getStyleClass().add("release-entry");
    entry.setCursor(Cursor.HAND);

    // Version Label
    Label versionLabel = new Label(release.getVersion());
    versionLabel.getStyleClass().add("release-version-label");
    versionLabel.setPrefWidth(100);

    // Date Label
    Label dateLabel = new Label(release.getDate());
    dateLabel.getStyleClass().add("release-date-label");
    dateLabel.setPrefWidth(120);

    // Status indicator (for selected state)
    Label statusLabel = new Label();
    statusLabel.getStyleClass().add("release-status-indicator");
    statusLabel.setPrefWidth(20);

    entry.getChildren().addAll(versionLabel, dateLabel, statusLabel);

    // Click handler
    entry.setOnMouseClicked(event -> selectRelease(release.getVersion()));

    return entry;
  }

  private void selectRelease(String version) {
    // Update visual selection
    updateReleaseSelection(version);

    // Update changelog display
    ReleaseData release = releasesData.get(version);
    if (release != null) {
      changelogTitle.setText("Changelog " + version);
      changelogText.setText(release.getChangelog());
      selectedReleaseVersion = version;

      log.debug("Selected release: {}", version);
    }
  }

  private void updateReleaseSelection(String selectedVersion) {
    releasesListContent
        .getChildren()
        .forEach(
            node -> {
              HBox entry = (HBox) node;
              Label versionLabel = (Label) entry.getChildren().get(0);
              Label statusLabel = (Label) entry.getChildren().get(2);

              if (versionLabel.getText().equals(selectedVersion)) {
                entry.getStyleClass().removeAll("release-entry");
                entry.getStyleClass().add("release-entry-selected");
                statusLabel.setText("●");
              } else {
                entry.getStyleClass().removeAll("release-entry-selected");
                entry.getStyleClass().add("release-entry");
                statusLabel.setText("");
              }
            });
  }

  @FXML
  void onDiscordOpen(MouseEvent event) {
    try {
      homeViewModel.openDiscord();
    } catch (IOException e) {
      log.error("Error opening discord", e);
      showAlertInternetConnection();
    }
  }

  @FXML
  void onGitHubOpen(MouseEvent event) {
    try {
      homeViewModel.openGitHub();
    } catch (IOException e) {
      log.error("Error opening github", e);
      showAlertInternetConnection();
    }
  }

  @FXML
  void onReleasesClick(MouseEvent event) {
    if (!isReleasesVisible) {
      showReleasesView();
    }
  }

  @FXML
  void onBackClick(MouseEvent event) {
    if (isReleasesVisible) {
      hideReleasesView();
    }
  }

  private void showReleasesView() {
    isReleasesVisible = true;

    // Get current height dynamically
    double currentHeight = rootPane.getHeight();

    // Parallele Animation für smooth transition
    ParallelTransition transition = new ParallelTransition();

    // Main Content nach oben animieren
    TranslateTransition mainUp = new TranslateTransition(ANIMATION_DURATION, mainContent);
    mainUp.setToY(-currentHeight);

    // Releases View von unten einblenden
    TranslateTransition releasesUp = new TranslateTransition(ANIMATION_DURATION, releasesView);
    releasesUp.setToY(-currentHeight);

    transition.getChildren().addAll(mainUp, releasesUp);
    transition.setInterpolator(Interpolator.EASE_BOTH);
    transition.play();

    log.debug("Showing releases view with height: {}", currentHeight);
  }

  private void hideReleasesView() {
    isReleasesVisible = false;

    ParallelTransition transition = new ParallelTransition();

    // Main Content zurück zur ursprünglichen Position
    TranslateTransition mainDown = new TranslateTransition(ANIMATION_DURATION, mainContent);
    mainDown.setToY(0);

    // Releases View nach unten ausblenden
    TranslateTransition releasesDown = new TranslateTransition(ANIMATION_DURATION, releasesView);
    releasesDown.setToY(0);

    transition.getChildren().addAll(mainDown, releasesDown);
    transition.setInterpolator(Interpolator.EASE_BOTH);

    // Nach der Animation die Position der releases view anpassen
    transition.setOnFinished(
        e -> {
          double currentHeight = rootPane.getHeight();
          AnchorPane.setTopAnchor(releasesView, currentHeight);
          AnchorPane.setBottomAnchor(releasesView, -currentHeight);
        });

    transition.play();

    log.debug("Hiding releases view");
  }

  private void showAlertInternetConnection() {
    Platform.runLater(
        () -> {
          Alert alert = new Alert(Alert.AlertType.ERROR);
          alert
              .getDialogPane()
              .getStylesheets()
              .add(RandomizerApplication.class.getResource("alert-style.css").toExternalForm());
          alert.setTitle("Internet Connectivity Issue");
          alert.setHeaderText(null);
          alert.setContentText("Please check your internet connection and try again.");
          alert.showAndWait();
        });
  }

  // Inner class for release data
  private static class ReleaseData {
    private final String version;
    private final String date;
    private final String changelog;

    public ReleaseData(String version, String date, String changelog) {
      this.version = version;
      this.date = date;
      this.changelog = changelog;
    }

    public String getVersion() {
      return version;
    }

    public String getDate() {
      return date;
    }

    public String getChangelog() {
      return changelog;
    }
  }
}
