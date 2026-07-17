package de.bsommerfeld.randomizer.config.crosshair;

import java.nio.file.Path;
import java.util.Map;

/**
 * A loaded crosshair: source file, the raw crosshair convars for display, and the typed settings.
 * Write-back does not go through this snapshot - saving re-reads the live file fresh (see
 * {@link de.bsommerfeld.randomizer.config.ConfigSaver}).
 */
public record Crosshair(Path source, Map<String, String> convars, CrosshairSettings settings) {
}
