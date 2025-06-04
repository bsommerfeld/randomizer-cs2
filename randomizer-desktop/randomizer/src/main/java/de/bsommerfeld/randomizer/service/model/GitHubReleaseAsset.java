package de.bsommerfeld.randomizer.service.model;

/**
 * Represents an asset attached to a GitHub release.
 */
public class GitHubReleaseAsset {
    private String name;
    private String url;
    private String contentType;
    private long size;
    
    public GitHubReleaseAsset(String name, String url, String contentType, long size) {
        this.name = name;
        this.url = url;
        this.contentType = contentType;
        this.size = size;
    }
    
    /**
     * @return The name of the asset
     */
    public String getName() {
        return name;
    }
    
    /**
     * @return The URL to download the asset
     */
    public String getUrl() {
        return url;
    }
    
    /**
     * @return The content type of the asset
     */
    public String getContentType() {
        return contentType;
    }
    
    /**
     * @return The size of the asset in bytes
     */
    public long getSize() {
        return size;
    }
    
    @Override
    public String toString() {
        return name;
    }
}