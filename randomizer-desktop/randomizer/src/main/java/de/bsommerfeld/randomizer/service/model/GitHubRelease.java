package de.bsommerfeld.randomizer.service.model;

import java.time.ZonedDateTime;
import java.util.List;

/** Represents a GitHub release with its associated information. */
public record GitHubRelease(String tag, ZonedDateTime releaseDate, String title, List<GitHubReleaseAsset> assets) {
  /**
   * Checks if this release has a CHANGELOG.md asset.
   *
   * @return true if the release has a CHANGELOG.md asset, false otherwise
   */
  public boolean hasChangelogAsset() {
    return assets.stream().anyMatch(asset -> "CHANGELOG.md".equals(asset.name()));
  }

  /**
   * Gets the CHANGELOG.md asset if it exists.
   *
   * @return The CHANGELOG.md asset, or null if it doesn't exist
   */
  public GitHubReleaseAsset getChangelogAsset() {
    return assets.stream()
        .filter(asset -> "CHANGELOG.md".equals(asset.name()))
        .findFirst()
        .orElse(null);
  }

  @Override
  public String toString() {
    return String.format(
        "%s:%s:%s", tag, releaseDate, hasChangelogAsset() ? "CHANGELOG.md" : "No CHANGELOG.md");
  }
}
