package de.bsommerfeld.randomizer.config.keybinds;

import de.bsommerfeld.randomizer.vdf.VdfObject;

import java.nio.file.Path;

/**
 * A loaded keybind config: its source file, the parsed {@link VdfObject} model (the editable
 * working copy, retained so edits can be staged) and the pretty JSON rendered for display.
 */
public record KeybindConfig(Path source, VdfObject model, String prettyJson) {
}
