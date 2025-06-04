package de.bsommerfeld.randomizer.service.model;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * Represents a GitHub release with its associated information.
 */
public class GitHubRelease {
    private String tag;
    private ZonedDateTime releaseDate;
    private String title;
    private List<GitHubReleaseAsset> assets;
    
    public GitHubRelease(String tag, ZonedDateTime releaseDate, String title, List<GitHubReleaseAsset> assets) {
        this.tag = tag;
        this.releaseDate = releaseDate;
        this.title = title;
        this.assets = assets;
    }
    
    /**
     * @return The tag name of the release
     */
    public String getTag() {
        return tag;
    }
    
    /**
     * @return The date when the release was published
     */
    public ZonedDateTime getReleaseDate() {
        return releaseDate;
    }
    
    /**
     * @return The title of the release
     */
    public String getTitle() {
        return title;
    }
    
    /**
     * @return The list of assets attached to the release
     */
    public List<GitHubReleaseAsset> getAssets() {
        return assets;
    }
    
    /**
     * Checks if this release has a CHANGELOG.md asset.
     * 
     * @return true if the release has a CHANGELOG.md asset, false otherwise
     */
    public boolean hasChangelogAsset() {
        return assets.stream()
                .anyMatch(asset -> "CHANGELOG.md".equals(asset.getName()));
    }
    
    /**
     * Gets the CHANGELOG.md asset if it exists.
     * 
     * @return The CHANGELOG.md asset, or null if it doesn't exist
     */
    public GitHubReleaseAsset getChangelogAsset() {
        return assets.stream()
                .filter(asset -> "CHANGELOG.md".equals(asset.getName()))
                .findFirst()
                .orElse(null);
    }
    
    @Override
    public String toString() {
        return String.format("%s:%s:%s", 
                tag, 
                releaseDate, 
                hasChangelogAsset() ? "CHANGELOG.md" : "No CHANGELOG.md");
    }
}