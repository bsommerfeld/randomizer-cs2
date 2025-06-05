package de.bsommerfeld.randomizer.ui.view.viewmodel;

import com.google.inject.Inject;
import de.bsommerfeld.randomizer.service.GitHubConfig;
import de.bsommerfeld.randomizer.service.GitHubService;
import de.bsommerfeld.randomizer.service.model.GitHubRepositoryDetails;
import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HomeViewModel {

  private static final String GITHUB_LINK = "https://github.com/bsommerfeld/randomizer-cs2";
  private static final String DISCORD_LINK = "https://discord.gg/782s5ExhFy";

  @Getter private final IntegerProperty starsProperty = new SimpleIntegerProperty();
  @Getter private final IntegerProperty forksProperty = new SimpleIntegerProperty();

  private final GitHubConfig gitHubConfig;
  private final GitHubService gitHubService;

  @Inject
  public HomeViewModel(GitHubConfig gitHubConfig, GitHubService gitHubService) {
    this.gitHubConfig = gitHubConfig;
    this.gitHubService = gitHubService;
  }

  public void openGitHub() throws IOException {
    Desktop.getDesktop().browse(URI.create(GITHUB_LINK));
  }

  public void openDiscord() throws IOException {
    Desktop.getDesktop().browse(URI.create(DISCORD_LINK));
  }

  public void updateRepositoryDetails() {
    log.info("Updating repository details..");
    CompletableFuture.runAsync(
            () -> {
              GitHubRepositoryDetails gitHubRepositoryDetails = null;
              try {
                gitHubRepositoryDetails =
                    gitHubService.getRepositoryDetails(
                        gitHubConfig.getAuthor(), gitHubConfig.getRepository());
              } catch (IOException | InterruptedException e) {
                log.error(e.getMessage());
                throw new RuntimeException(e);
              }
              final int stars = gitHubRepositoryDetails.stargazersCount();
              final int forks = gitHubRepositoryDetails.forksCount();

              log.info("Gathered repository details successfully - updating view..");

              Platform.runLater(
                  () -> {
                    starsProperty.set(stars);
                    forksProperty.set(forks);

                    log.info("View successfully updated with {} stars and {} forks", stars, forks);
                  });
            })
        .exceptionally(
            throwable -> {
              log.error(
                  "Failed to query GitHub information from {}/{}",
                  gitHubConfig.getAuthor(),
                  gitHubConfig.getRepository());
              return null;
            });
  }
}
