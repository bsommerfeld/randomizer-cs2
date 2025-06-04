package de.bsommerfeld.randomizer.service.model;

/** Represents an asset attached to a GitHub release. */
public record GitHubReleaseAsset(String name, String url, String contentType, long size) {}
