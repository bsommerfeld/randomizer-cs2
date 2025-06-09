package de.bsommerfeld.randomizer.ui.view.controller;

import com.google.inject.Inject;
import de.bsommerfeld.github.model.GitHubRelease;
import de.bsommerfeld.randomizer.ui.RandomizerApplication;
import de.bsommerfeld.randomizer.ui.view.View;
import de.bsommerfeld.randomizer.ui.view.viewmodel.HomeViewModel;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
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
  @FXML private Label forksLabel;

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
    setupGitHubDetailsBindings();
    setupReleasesData();

    try {
      homeViewModel.updateReleases();
      log.debug("Release update initiated successfully");
    } catch (Exception e) {
      log.error("Failed to update releases: {}", e.getMessage(), e);
      showReleaseLoadingError();
    }
  }

  private void showReleaseLoadingError() {
    Platform.runLater(() -> {
      Label errorLabel = new Label("Fehler beim Laden der Releases");
      errorLabel.getStyleClass().add("error-label");
      releasesListContent.getChildren().clear();
      releasesListContent.getChildren().add(errorLabel);
    });
  }

  @FXML
  private void onRepositoryDetailsClick(MouseEvent event) {
    onGitHubOpen(event);
  }

  private void setupGitHubDetailsBindings() {
    starsLabel.textProperty().bind(homeViewModel.getStarsProperty().asString());
    forksLabel.textProperty().bind(homeViewModel.getForksProperty().asString());
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
    homeViewModel.getReleasesList().addListener((ListChangeListener<GitHubRelease>) change -> {
      while (change.next()) { // ← Das war das fehlende Stück!
        if (change.wasAdded()) {
          change.getAddedSubList().forEach(release -> {
            log.debug("Processing release: {}", release.tag());

            homeViewModel.fetchChangelog(release)
                    .thenAcceptAsync(changelog -> {
                      Platform.runLater(() -> {
                        releasesData.put(release.tag(),
                                new ReleaseData(release.title(),
                                        release.releaseDate().toString(),
                                        changelog));
                        populateReleasesList();
                        log.debug("Added release: {}", release.tag());
                      });
                    })
                    .exceptionally(throwable -> {
                      log.error("Error loading changelog for release {}: {}",
                              release.tag(), throwable.getMessage());
                      Platform.runLater(() -> {
                        releasesData.put(release.tag(),
                                new ReleaseData(release.title(),
                                        release.releaseDate().toString(),
                                        "Changelog could not be loaded"));
                        populateReleasesList();
                      });
                      return null;
                    });
          });
        }

        if (change.wasRemoved()) {
          change.getRemoved().forEach(release -> {
            releasesData.remove(release.tag());
            populateReleasesList();
            log.debug("Removed release: {}", release.tag());
          });
        }
      }
    });
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
