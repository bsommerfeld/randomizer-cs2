package de.bsommerfeld.randomizer.config.crosshair;

import de.bsommerfeld.randomizer.config.AppDirectories;
import de.bsommerfeld.randomizer.vdf.VdfParseException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

/**
 * Persists the user's own ("standard") crosshair on disk as a backup, separate from the live
 * {@code cs2_user_convars.vcfg}. Stored as an exact byte copy under
 * {@code %LOCALAPPDATA%\randomizer-cs2\backup}, so nothing (comments, unrelated convars) is lost,
 * and read back through the normal crosshair parser. This is what "reset" restores to, and it
 * survives even if the original file is later changed.
 */
public final class CrosshairBackup {

    private static final String FILE_NAME = "cs2_user_convars.vcfg";

    private final Path backupFile;
    private final CrosshairConfigParser parser;

    public CrosshairBackup() {
        this(AppDirectories.base().resolve("backup"), new CrosshairConfigParser());
    }

    public CrosshairBackup(Path backupDir, CrosshairConfigParser parser) {
        this.backupFile = backupDir.resolve(FILE_NAME);
        this.parser = parser;
    }

    /** Whether a backup of the user's standard crosshair exists. */
    public boolean exists() {
        return Files.isRegularFile(backupFile);
    }

    /** Saves {@code source} as the standard backup - an exact copy, replacing any previous backup. */
    public void save(Path source) throws IOException {
        Files.createDirectories(backupFile.getParent());
        Files.copy(source, backupFile, StandardCopyOption.REPLACE_EXISTING);
    }

    /** Restores the backup byte-exactly over {@code target}; fails if no backup exists. */
    public void restoreTo(Path target) throws IOException {
        Files.copy(backupFile, target, StandardCopyOption.REPLACE_EXISTING);
    }

    /** The backed-up crosshair, or empty if there is none or it cannot be read. */
    public Optional<Crosshair> read() {
        if (!Files.isRegularFile(backupFile)) {
            return Optional.empty();
        }
        try {
            return Optional.of(parser.parse(backupFile));
        } catch (IOException | VdfParseException e) {
            return Optional.empty();
        }
    }
}
