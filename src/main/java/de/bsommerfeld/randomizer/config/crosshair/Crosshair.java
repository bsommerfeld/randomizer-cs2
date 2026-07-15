package de.bsommerfeld.randomizer.config.crosshair;

import java.nio.file.Path;
import java.util.Map;

/** A loaded crosshair: source file, the raw convars for display, and the parsed settings. */
public record Crosshair(Path source, Map<String, String> convars, CrosshairSettings settings) {
}
