package de.bsommerfeld.randomizer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Singleton;
import de.bsommerfeld.randomizer.service.model.GitHubRelease;
import de.bsommerfeld.randomizer.service.model.GitHubReleaseAsset;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for interacting with GitHub API to fetch repository releases.
 */
@Singleton
public class GitHubService {

    private static final String GITHUB_API_URL = "https://api.github.com";
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GitHubService() {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Fetches all releases for a specific GitHub repository.
     * 
     * @param owner The owner of the repository
     * @param repo The name of the repository
     * @return A list of releases for the repository
     * @throws IOException If an I/O error occurs
     * @throws InterruptedException If the operation is interrupted
     */
    public List<GitHubRelease> getRepositoryReleases(String owner, String repo) 
            throws IOException, InterruptedException {
        String apiUrl = String.format("%s/repos/%s/%s/releases", GITHUB_API_URL, owner, repo);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Accept", "application/vnd.github.v3+json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Failed to fetch releases: HTTP " + response.statusCode());
        }

        return parseReleases(response.body());
    }

    /**
     * Fetches all releases for a specific GitHub repository that have a CHANGELOG.md attachment.
     * 
     * @param owner The owner of the repository
     * @param repo The name of the repository
     * @return A list of releases for the repository that have a CHANGELOG.md attachment
     * @throws IOException If an I/O error occurs
     * @throws InterruptedException If the operation is interrupted
     */
    public List<GitHubRelease> getRepositoryReleasesWithChangelog(String owner, String repo) 
            throws IOException, InterruptedException {
        List<GitHubRelease> allReleases = getRepositoryReleases(owner, repo);
        return allReleases.stream()
                .filter(GitHubRelease::hasChangelogAsset)
                .collect(Collectors.toList());
    }

    /**
     * Parses the JSON response from the GitHub API into a list of GitHubRelease objects.
     * 
     * @param json The JSON response from the GitHub API
     * @return A list of GitHubRelease objects
     */
    private List<GitHubRelease> parseReleases(String json) {
        List<GitHubRelease> releases = new ArrayList<>();

        try {
            JsonNode rootNode = objectMapper.readTree(json);
            if (rootNode.isArray()) {
                for (JsonNode releaseNode : rootNode) {
                    String tag = releaseNode.get("tag_name").asText();
                    String title = releaseNode.get("name").asText();
                    ZonedDateTime releaseDate = ZonedDateTime.parse(
                            releaseNode.get("published_at").asText(),
                            DateTimeFormatter.ISO_DATE_TIME);

                    List<GitHubReleaseAsset> assets = parseAssets(releaseNode.get("assets"));

                    releases.add(new GitHubRelease(tag, releaseDate, title, assets));
                }
            }
        } catch (Exception e) {
            // Log the error and return an empty list
            System.err.println("Error parsing GitHub releases: " + e.getMessage());
            return Collections.emptyList();
        }

        return releases;
    }

    /**
     * Parses the assets JSON array into a list of GitHubReleaseAsset objects.
     * 
     * @param assetsNode The JSON array of assets
     * @return A list of GitHubReleaseAsset objects
     */
    private List<GitHubReleaseAsset> parseAssets(JsonNode assetsNode) {
        List<GitHubReleaseAsset> assets = new ArrayList<>();

        if (assetsNode != null && assetsNode.isArray()) {
            for (JsonNode assetNode : assetsNode) {
                String name = assetNode.get("name").asText();
                String url = assetNode.get("browser_download_url").asText();
                String contentType = assetNode.get("content_type").asText();
                long size = assetNode.get("size").asLong();

                assets.add(new GitHubReleaseAsset(name, url, contentType, size));
            }
        }

        return assets;
    }

    /**
     * Downloads the content of a release asset.
     * 
     * @param asset The asset to download
     * @return The content of the asset as a string
     * @throws IOException If an I/O error occurs
     */
    public String downloadAssetContent(GitHubReleaseAsset asset) throws IOException {
        URL url = new URL(asset.getUrl());
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "application/octet-stream");

        try (InputStream inputStream = connection.getInputStream()) {
            return new String(inputStream.readAllBytes());
        }
    }
}
