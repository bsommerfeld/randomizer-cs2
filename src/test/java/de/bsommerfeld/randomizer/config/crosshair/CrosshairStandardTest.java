package de.bsommerfeld.randomizer.config.crosshair;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrosshairStandardTest {

    @TempDir
    Path tempDir;

    private final CrosshairConfigParser parser = new CrosshairConfigParser();
    private CrosshairBackup backup;
    private CrosshairStandard standard;
    private int fileCounter;

    private CrosshairStandard standard() {
        backup = new CrosshairBackup(tempDir.resolve("backup"), parser);
        standard = new CrosshairStandard(backup);
        return standard;
    }

    /** A loaded crosshair whose {@code cl_crosshairsize} is {@code size}, from a unique source file. */
    private Optional<Crosshair> crosshair(String size) throws IOException {
        Path file = tempDir.resolve("convars-" + fileCounter++ + ".vcfg");
        Files.writeString(file, "\"convars\"\n{\n\t\"cl_crosshairsize\" \"" + size + "\"\n}\n");
        return Optional.of(parser.parse(file));
    }

    @Test
    void firstLoadBacksUpTheCrosshairAndReturnsItsValues() throws IOException {
        CrosshairStandard standard = standard();

        var values = standard.resolve(crosshair("3"), false);

        assertTrue(backup.exists(), "the user crosshair is saved to disk on first sight");
        assertEquals("3", values.get("cl_crosshairsize"));
    }

    @Test
    void autoLoadDoesNotOverwriteAnExistingBackup() throws IOException {
        CrosshairStandard standard = standard();
        standard.resolve(crosshair("3"), false); // captures 3 as the standard

        // A later auto-load of a different crosshair must NOT replace the saved standard.
        var values = standard.resolve(crosshair("1"), false);

        assertEquals("3", values.get("cl_crosshairsize"), "reset target stays the user's standard");
    }

    @Test
    void explicitFileChoiceUpdatesTheBackup() throws IOException {
        CrosshairStandard standard = standard();
        standard.resolve(crosshair("3"), false);

        var values = standard.resolve(crosshair("1"), true); // user picked a file → new standard

        assertEquals("1", values.get("cl_crosshairsize"));
    }

    @Test
    void withoutALoadedFileTheBackupIsTheStandard() throws IOException {
        CrosshairStandard standard = standard();
        standard.resolve(crosshair("3"), false);

        var values = standard.resolve(Optional.empty(), false);

        assertEquals("3", values.get("cl_crosshairsize"),
                "reset falls back to the saved user standard, not CS2 defaults");
    }

    @Test
    void restoreToWritesTheBackupOverTheLiveFile() throws IOException {
        CrosshairStandard standard = standard();
        Optional<Crosshair> loaded = crosshair("3");
        Path liveFile = loaded.orElseThrow().source();
        standard.resolve(loaded, false); // captures 3 as the standard
        Files.writeString(liveFile, "\"convars\"\n{\n\t\"cl_crosshairsize\" \"9\"\n}\n");

        standard.restoreTo(liveFile);

        assertEquals("3", parser.parse(liveFile).convars().get("cl_crosshairsize"));
    }

    @Test
    void withNeitherFileNorBackupCs2DefaultsApply() {
        CrosshairStandard standard = standard();

        var values = standard.resolve(Optional.empty(), false);

        assertFalse(standard.hasBackup());
        assertEquals(CrosshairConvar.defaults(), values);
    }
}
