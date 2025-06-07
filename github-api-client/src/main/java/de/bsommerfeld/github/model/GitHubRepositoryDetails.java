package de.bsommerfeld.github.model;

/**
 * Represents basic details of a GitHub repository.
 *
 * @param name The name of the repository.
 * @param description The description of the repository.
 * @param stargazersCount The number of stargazers.
 * @param forksCount The number of forks.
 * @param htmlUrl The HTML URL to the repository.
 */
public record GitHubRepositoryDetails(
    String name, String description, int stargazersCount, int forksCount, String htmlUrl) {}
