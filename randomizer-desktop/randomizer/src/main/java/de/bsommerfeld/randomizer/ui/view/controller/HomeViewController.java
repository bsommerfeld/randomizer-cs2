package de.bsommerfeld.randomizer.ui.view.controller;

import com.google.inject.Inject;
import de.bsommerfeld.github.model.GitHubRelease;
import de.bsommerfeld.randomizer.ui.RandomizerApplication;
import de.bsommerfeld.randomizer.ui.view.View;
import de.bsommerfeld.randomizer.ui.view.viewmodel.HomeViewModel;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
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
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@View
public class HomeViewController {

  private static final DateTimeFormatter DATE_FORMATTER =
      DateTimeFormatter.ofPattern("dd.MM.yyy HH:mm");
  private static final Duration ANIMATION_DURATION = Duration.millis(400);

  private final HomeViewModel homeViewModel;
  private final Map<String, ReleaseData> releasesData = new HashMap<>();

  @FXML private AnchorPane rootPane;
  @FXML private VBox mainContent;
  @FXML private Label starsLabel;
  @FXML private Label forksLabel;

  @FXML private VBox releasesView;
  @FXML private VBox releasesListContent;
  @FXML private Label changelogTitle;
  @FXML private TextFlow changelogTextFlow;
  private boolean isReleasesVisible = false;

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
      updateView();
      log.debug("GitHub update initiated successfully");
    } catch (Exception e) {
      log.error("Failed to update GitHub: {}", e.getMessage(), e);
      showReleaseLoadingError();
    }

    if(releasesData.isEmpty()) {
      showReleaseLoadingError();
    }
  }

  /** Shows an error message when releases cannot be loaded */
  private void showReleaseLoadingError() {
    Platform.runLater(
        () -> {
          Label errorLabel = new Label("Fehler beim Laden der Releases");
          errorLabel.getStyleClass().add("error-label");
          releasesListContent.getChildren().clear();
          releasesListContent.getChildren().add(errorLabel);

          // Clear changelog and show error message
          changelogTextFlow.getChildren().clear();
          Text errorText =
              new Text(
                  "Could not load release information. Please check your internet connection and try again.");
          errorText.getStyleClass().add("changelog-placeholder");
          changelogTextFlow.getChildren().add(errorText);
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

  private void updateView() {
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
    homeViewModel
        .getReleasesList()
        .addListener(
            (ListChangeListener<GitHubRelease>)
                change -> {
                  while (change.next()) {
                    if (change.wasAdded()) {
                      change.getAddedSubList().forEach(this::handleReleaseAdded);
                    }

                    if (change.wasRemoved()) {
                      change.getRemoved().forEach(this::handleReleaseRemoved);
                    }
                  }
                });
  }

  private void handleReleaseAdded(GitHubRelease release) {
    log.debug("Processing release: {}", release.tag());

    homeViewModel
        .fetchChangelog(release)
        .thenAcceptAsync(changelog -> handleChangelogLoaded(release, changelog))
        .exceptionally(throwable -> handleChangelogError(release, throwable));
  }

  private void handleChangelogLoaded(GitHubRelease release, String changelog) {
    Platform.runLater(
        () -> {
          String formattedDate = formatReleaseDate(release);
          ReleaseData releaseData = new ReleaseData(release.title(), formattedDate, changelog);

          releasesData.put(release.tag(), releaseData);
          populateReleasesList();
          log.debug("Added release: {}", release.tag());
        });
  }

  private Void handleChangelogError(GitHubRelease release, Throwable throwable) {
    log.error("Error loading changelog for release {}: {}", release.tag(), throwable.getMessage());

    Platform.runLater(
        () -> {
          String formattedDate = formatReleaseDate(release);
          ReleaseData releaseData =
              new ReleaseData(release.title(), formattedDate, "Changelog could not be loaded");

          releasesData.put(release.tag(), releaseData);
          populateReleasesList();
        });
    return null;
  }

  private void handleReleaseRemoved(GitHubRelease release) {
    releasesData.remove(release.tag());
    populateReleasesList();
    log.debug("Removed release: {}", release.tag());
  }

  private String formatReleaseDate(GitHubRelease release) {
    return release.releaseDate().format(DATE_FORMATTER);
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

  /**
   * Creates a release entry HBox with version, date, and status indicator.
   *
   * @param release The release data to display
   * @return An HBox containing the release entry UI
   */
  private HBox createReleaseEntry(ReleaseData release) {
    HBox entry = new HBox();
    entry.getStyleClass().add("release-entry");
    entry.setCursor(Cursor.HAND);

    // Version Label
    Label versionLabel = new Label(release.getVersion());
    versionLabel.getStyleClass().add("release-version-label");

    // Date Label
    Label dateLabel = new Label(release.getDate());
    dateLabel.getStyleClass().add("release-date-label");

    // Status indicator (for selected state)
    Label statusLabel = new Label();
    statusLabel.getStyleClass().add("release-status-indicator");

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
      renderMarkdown(release.getChangelog());

      log.debug("Selected release: {}", version);
    }
  }

  /**
   * Renders markdown text in the changelog TextFlow. Supports basic markdown features like headers,
   * lists, and code blocks.
   *
   * @param markdown The markdown text to render
   */
  private void renderMarkdown(String markdown) {
    changelogTextFlow.getChildren().clear();

    if (markdown == null || markdown.isEmpty()) {
      addPlaceholderText();
      return;
    }

    String[] lines = markdown.split("\\n");
    for (String line : lines) {
      if (line.trim().isEmpty()) {
        addEmptyLine();
        continue;
      }

      // Process the line based on its markdown syntax
      if (line.startsWith("# ")) {
        addHeader(line.substring(2), 24);
      } else if (line.startsWith("## ")) {
        addHeader(line.substring(3), 20);
      } else if (line.startsWith("### ")) {
        addHeader(line.substring(4), 18);
      } else if (line.startsWith("- ") || line.startsWith("* ")) {
        addListItem(line.substring(2));
      } else if (line.contains("`")) {
        processLineWithCode(line);
      } else {
        addNormalText(line);
      }
    }

    // Log successful rendering
    log.debug("Rendered markdown with {} lines", lines.length);
  }

  /** Adds a placeholder text when no changelog is available */
  private void addPlaceholderText() {
    Text placeholder = new Text("No changelog available for this release.");
    placeholder.getStyleClass().add("changelog-placeholder");
    changelogTextFlow.getChildren().add(placeholder);
  }

  /** Adds an empty line to the TextFlow */
  private void addEmptyLine() {
    changelogTextFlow.getChildren().add(new Text("\n"));
  }

  /**
   * Adds a header text with the specified font size
   *
   * @param text The header text
   * @param fontSize The font size to use
   */
  private void addHeader(String text, int fontSize) {
    Text header = new Text(text + "\n");
    header.setFont(Font.font("Segoe UI", FontWeight.BOLD, fontSize));
    header.setFill(Color.web("#443DFF"));
    changelogTextFlow.getChildren().add(header);
  }

  /**
   * Adds a list item with a bullet point
   *
   * @param text The list item text
   */
  private void addListItem(String text) {
    Text bullet = new Text("• ");
    bullet.setFill(Color.web("#443DFF"));

    Text listItem = new Text(text + "\n");

    changelogTextFlow.getChildren().addAll(bullet, listItem);
  }

  /**
   * Adds normal text to the TextFlow
   *
   * @param text The text to add
   */
  private void addNormalText(String text) {
    changelogTextFlow.getChildren().add(new Text(text + "\n"));
  }

  /**
   * Processes a line that contains inline code (text surrounded by backticks)
   *
   * @param line The line to process
   */
  private void processLineWithCode(String line) {
    String[] parts = line.split("`");
    for (int i = 0; i < parts.length; i++) {
      if (i % 2 == 0) {
        // Regular text
        if (!parts[i].isEmpty()) {
          changelogTextFlow.getChildren().add(new Text(parts[i]));
        }
      } else {
        // Code
        Text codeText = new Text(parts[i]);
        codeText.setFont(Font.font("Monospaced", 14));
        codeText.setFill(Color.web("#7E78ED"));
        codeText.setStyle("-fx-background-color: #F5F5F5;");
        changelogTextFlow.getChildren().add(codeText);
      }
    }
    // Add newline
    changelogTextFlow.getChildren().add(new Text("\n"));
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
