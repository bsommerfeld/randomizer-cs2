package de.bsommerfeld.randomizer.ui.view.viewmodel;

import com.google.inject.Inject;
import de.bsommerfeld.github.config.GitHubConfig;
import de.bsommerfeld.github.model.GitHubRelease;
import de.bsommerfeld.github.service.GitHubService;
import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HomeViewModel {

  private static final String GITHUB_LINK = "https://github.com/bsommerfeld/randomizer-cs2";
  private static final String DISCORD_LINK = "https://discord.gg/782s5ExhFy";

  private final ObservableList<GitHubRelease> releasesList = FXCollections.observableArrayList();

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

  public ObservableList<GitHubRelease> getReleasesList() {
    return FXCollections.unmodifiableObservableList(releasesList);
  }

  public void updateReleases() {
    log.info("Updating releases...");
    CompletableFuture.supplyAsync(
            () -> {
              try {
                return gitHubService.getRepositoryReleasesWithChangelog(
                    gitHubConfig.getAuthor(), gitHubConfig.getRepository());
              } catch (IOException | InterruptedException e) {
                log.error("Error updating releases: {}", e.getMessage(), e);
                throw new RuntimeException(e);
              }
            })
        .thenAcceptAsync(releasesList::setAll, Platform::runLater);
  }

  public void updateRepositoryDetails() {
    log.info("Updating repository details..");
    CompletableFuture.supplyAsync(
            () -> {
              try {
                return gitHubService.getRepositoryDetails(
                    gitHubConfig.getAuthor(), gitHubConfig.getRepository());
              } catch (IOException | InterruptedException e) {
                log.error("Error updating repository details: {} ", e.getMessage());
                throw new RuntimeException(e);
              }
            })
        .thenAcceptAsync(
            gitHubRepositoryDetails -> {
              log.info("Gathered repository details successfully - updating view..");

              final int stars = gitHubRepositoryDetails.stargazersCount();
              final int forks = gitHubRepositoryDetails.forksCount();

              starsProperty.set(stars);
              forksProperty.set(forks);

              log.info("View successfully updated with {} stars and {} forks", stars, forks);
            },
            Platform::runLater);
  }
}
