package de.bsommerfeld.randomizer.config.crosshair;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrosshairBackupTest {

    @TempDir
    Path tempDir;

    private CrosshairBackup backup() {
        return new CrosshairBackup(tempDir.resolve("backup"), new CrosshairConfigParser());
    }

    private Path convarsFile(String size) throws IOException {
        Path file = tempDir.resolve("cs2_user_convars.vcfg");
        Files.writeString(file, "\"convars\"\n{\n\t\"cl_crosshairsize\" \"" + size + "\"\n}\n");
        return file;
    }

    @Test
    void doesNotExistInitially() {
        assertFalse(backup().exists());
        assertTrue(backup().read().isEmpty());
    }

    @Test
    void saveThenReadReturnsTheBackedUpCrosshair() throws IOException {
        CrosshairBackup backup = backup();

        backup.save(convarsFile("3"));

        assertTrue(backup.exists());
        assertEquals("3", backup.read().orElseThrow().convars().get("cl_crosshairsize"));
    }

    @Test
    void saveIsAnExactCopyOfTheSource() throws IOException {
        Path source = convarsFile("5");
        CrosshairBackup backup = backup();

        backup.save(source);

        Path backupFile = tempDir.resolve("backup/cs2_user_convars.vcfg");
        assertEquals(Files.readString(source), Files.readString(backupFile));
    }

    @Test
    void restoreToOverwritesTargetWithTheExactBackupBytes() throws IOException {
        Path source = convarsFile("3");
        CrosshairBackup backup = backup();
        backup.save(source);
        String original = Files.readString(source);
        Files.writeString(source, "\"convars\"\n{\n\t\"cl_crosshairsize\" \"9\"\n}\n");

        backup.restoreTo(source);

        assertEquals(original, Files.readString(source));
    }

    @Test
    void restoreToWithoutBackupThrows() throws IOException {
        Path target = convarsFile("3");

        assertThrows(NoSuchFileException.class, () -> backup().restoreTo(target));
    }

    @Test
    void saveReplacesAnEarlierBackup() throws IOException {
        CrosshairBackup backup = backup();
        backup.save(convarsFile("3"));

        backup.save(convarsFile("1"));

        assertEquals("1", backup.read().orElseThrow().convars().get("cl_crosshairsize"));
    }
}
