package de.bsommerfeld.randomizer.config.keybinds;

import java.nio.file.Path;

/** A loaded keybind config: its source file and the pretty JSON rendered for display. */
public record KeybindConfig(Path source, String prettyJson) {
}
