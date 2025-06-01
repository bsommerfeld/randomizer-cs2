package de.bsommerfeld.randomizer.ui.view.controller;

import com.google.inject.Inject;
import de.bsommerfeld.randomizer.config.RandomizerConfig;
import de.bsommerfeld.randomizer.ui.util.GifDecoder;
import de.bsommerfeld.randomizer.ui.view.View;
import de.bsommerfeld.randomizer.ui.view.ViewProvider;
import de.bsommerfeld.randomizer.ui.view.controller.builder.BuilderViewController;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.util.Duration;

@View
public class RandomizerWindowController implements Initializable {

  private static final String GIF_RESOURCE_PATH = "de/bsommerfeld/randomizer/gif/its-time.gif";

  private final ViewProvider viewProvider;
  private final RandomizerConfig randomizerConfig;

  @FXML private BorderPane root;
  @FXML private GridPane contentPane;

  @Inject
  public RandomizerWindowController(ViewProvider viewProvider, RandomizerConfig randomizerConfig) {
    this.viewProvider = viewProvider;
    this.randomizerConfig = randomizerConfig;
  }

  @Override
  public void initialize(URL url, ResourceBundle resourceBundle) {
    registerViewListener();

    if (randomizerConfig.isShowIntro()) addPreloadingGif();

    loadHomeView();
  }

  private void addPreloadingGif() {
    try {
      GifDecoder gifDecoder = new GifDecoder(GIF_RESOURCE_PATH);
      ImageView e = new ImageView(new Image(GIF_RESOURCE_PATH));

      e.fitHeightProperty().bind(root.heightProperty());
      e.fitWidthProperty().bind(root.widthProperty());

      root.getChildren().add(e);
      executeAfterDelay(gifDecoder.getTotalDuration(), () -> root.getChildren().remove(e));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void executeAfterDelay(int millis, Runnable action) {
    Timeline delay = new Timeline(new KeyFrame(Duration.millis(millis), _ -> action.run()));
    delay.setCycleCount(1);
    delay.play();
  }

  private void registerViewListener() {
    viewProvider.registerViewChangeListener(HomeViewController.class, _ -> loadHomeView());
    viewProvider.registerViewChangeListener(BuilderViewController.class, _ -> loadBuilderView());
    viewProvider.registerViewChangeListener(
        RandomizerViewController.class, _ -> loadRandomizerView());
    viewProvider.registerViewChangeListener(SettingsViewController.class, _ -> loadSettingsView());
  }

  private void loadSettingsView() {
    Parent settingsViewParent = viewProvider.requestView(SettingsViewController.class).parent();
    setContent(settingsViewParent);
  }

  private void loadHomeView() {
    Parent homeViewParent = viewProvider.requestView(HomeViewController.class).parent();
    setContent(homeViewParent);
  }

  private void loadRandomizerView() {
    Parent randomizerViewParent = viewProvider.requestView(RandomizerViewController.class).parent();
    setContent(randomizerViewParent);
  }

  private void loadBuilderView() {
    Parent builderViewParent = viewProvider.requestView(BuilderViewController.class).parent();
    setContent(builderViewParent);
  }

  private void setContent(Node node) {
    clearContent();
    if (node != null) {
      contentPane.getChildren().add(node);
    }
  }

  private void clearContent() {
    contentPane.getChildren().clear();
  }
}
