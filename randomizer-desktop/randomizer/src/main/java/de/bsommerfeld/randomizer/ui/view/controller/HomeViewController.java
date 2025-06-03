package de.bsommerfeld.randomizer.ui.view.controller;

import com.google.inject.Inject;
import de.bsommerfeld.randomizer.ui.RandomizerApplication;
import de.bsommerfeld.randomizer.ui.view.View;
import de.bsommerfeld.randomizer.ui.view.viewmodel.HomeViewModel;
import java.io.IOException;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@View
public class HomeViewController {

    private static final Duration ANIMATION_DURATION = Duration.millis(400);
    private final HomeViewModel homeViewModel;
    @FXML private AnchorPane rootPane;
    @FXML private VBox mainContent;
    @FXML private VBox releasesView;
    private boolean isReleasesVisible = false;

    @Inject
    public HomeViewController(HomeViewModel homeViewModel) {
        this.homeViewModel = homeViewModel;
    }

    @FXML
    private void initialize() {
        setupResponsiveLayout();
    }

    private void setupResponsiveLayout() {
        // Initial positioning of releases view below visible area
        rootPane.heightProperty().addListener((observable, oldValue, newValue) -> {
            if (!isReleasesVisible) {
                // Keep releases view positioned below the visible area
                double height = newValue.doubleValue();
                AnchorPane.setTopAnchor(releasesView, height);
                AnchorPane.setBottomAnchor(releasesView, -height);
            }
        });

        // Set initial position when the scene is ready
        Platform.runLater(() -> {
            if (rootPane.getHeight() > 0) {
                double height = rootPane.getHeight();
                AnchorPane.setTopAnchor(releasesView, height);
                AnchorPane.setBottomAnchor(releasesView, -height);
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
        transition.setOnFinished(e -> {
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
}