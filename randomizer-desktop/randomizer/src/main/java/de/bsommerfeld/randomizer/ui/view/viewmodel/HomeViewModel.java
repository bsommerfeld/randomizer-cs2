package de.bsommerfeld.randomizer.ui.view.viewmodel;

import com.google.inject.Inject;
import de.bsommerfeld.github.config.GitHubConfig;
import de.bsommerfeld.github.model.GitHubRelease;
import de.bsommerfeld.github.model.GitHubReleaseAsset;
import de.bsommerfeld.github.service.GitHubService;
import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HomeViewModel {

  private static final String GITHUB_LINK = "https://github.com/bsommerfeld/randomizer-cs2";
  private static final String DISCORD_LINK = "https://discord.gg/782s5ExhFy";

  @Getter
  private final ObservableList<GitHubRelease> releasesList = FXCollections.observableArrayList();

  @Getter private final StringProperty currentChangelogProperty = new SimpleStringProperty();
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

  public CompletionStage<String> fetchChangelog(GitHubRelease gitHubRelease) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        GitHubReleaseAsset changelogAsset = gitHubRelease.getChangelogAsset();
        if (changelogAsset == null) {
          return "Kein Changelog verfügbar für diese Version";
        }
        return gitHubService.downloadAssetContent(changelogAsset);
      } catch (IOException e) {
        log.error("Error downloading changelog for {}: {}", gitHubRelease.tag(), e.getMessage());
        throw new RuntimeException("Fehler beim Laden des Changelogs: " + e.getMessage(), e);
      }
    });
  }

  public void updateReleases() {
    log.info("Updating releases...");
    releasesList.clear();
    CompletableFuture.supplyAsync(
            () -> {
              try {
                List<GitHubRelease> repositoryReleasesWithChangelog = gitHubService.getRepositoryReleasesWithChangelog(
                        gitHubConfig.getAuthor(), gitHubConfig.getRepository());
                log.info("Fetched {} releases with an CHANGELOG.md", repositoryReleasesWithChangelog.size());
                return repositoryReleasesWithChangelog;
              } catch (IOException | InterruptedException e) {
                log.error("Error updating releases: {}", e.getMessage(), e);
                throw new RuntimeException(e);
              }
            })
        .thenAcceptAsync(releasesList::addAll, Platform::runLater);
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
