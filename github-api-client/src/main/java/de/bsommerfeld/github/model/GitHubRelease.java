package de.bsommerfeld.github.model;

import java.text.MessageFormat;
import java.time.ZonedDateTime;
import java.util.List;

/** Represents a GitHub release with its associated information. */
public record GitHubRelease(
    String tag, ZonedDateTime releaseDate, String title, List<GitHubReleaseAsset> assets) {

  /**
   * Gets the CHANGELOG.md asset if it exists.
   *
   * @return The CHANGELOG.md asset, or null if it doesn't exist
   */
  public GitHubReleaseAsset getChangelogAsset() {
    return assets.stream()
        .filter(asset -> "CHANGELOG.md".equals(asset.name()))
        .findFirst()
        .orElseThrow(
            () ->
                new IllegalStateException(
                    MessageFormat.format("No CHANGELOG.md asset found for {0}", this.tag)));
  }
}
