package de.bsommerfeld.randomizer.config;

import de.bsommerfeld.randomizer.vdf.VdfObject;
import de.bsommerfeld.randomizer.vdf.VdfParser;
import de.bsommerfeld.randomizer.vdf.VdfWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigSaverTest {

    @TempDir
    Path tempDir;

    private final ConfigSaver saver = new ConfigSaver();

    private Path writeConfig(String content) throws IOException {
        Path file = tempDir.resolve("cs2_user_convars.vcfg");
        Files.writeString(file, content);
        return file;
    }

    @Test
    void replacesTargetKeysAndPreservesUnrelatedConvars() throws IOException {
        Path file = writeConfig("""
                "UserConvars"
                {
                \t"convars"
                \t{
                \t\t"cl_crosshairsize"\t\t"3"
                \t\t"sensitivity"\t\t"1.5"
                \t}
                }
                """);

        saver.save(file, Map.of("cl_crosshairsize", "7"));

        VdfObject convars = VdfParser.parse(file)
                .getObject("UserConvars").orElseThrow()
                .getObject("convars").orElseThrow();
        assertEquals("7", convars.getString("cl_crosshairsize").orElseThrow());
        assertEquals("1.5", convars.getString("sensitivity").orElseThrow());
    }

    @Test
    void appendsMissingKeysNextToExistingTargetKeys() throws IOException {
        Path file = writeConfig("""
                "UserConvars"
                {
                \t"convars"
                \t{
                \t\t"cl_crosshairsize"\t\t"3"
                \t}
                }
                """);
        Map<String, String> values = new LinkedHashMap<>();
        values.put("cl_crosshairsize", "5");
        values.put("cl_crosshairgap", "-2");

        saver.save(file, values);

        VdfObject convars = VdfParser.parse(file)
                .getObject("UserConvars").orElseThrow()
                .getObject("convars").orElseThrow();
        assertEquals("5", convars.getString("cl_crosshairsize").orElseThrow());
        assertEquals("-2", convars.getString("cl_crosshairgap").orElseThrow());
    }

    @Test
    void appendsIntoConvarsNodeWhenNoTargetKeyExistsYet() throws IOException {
        Path file = writeConfig("""
                "UserConvars"
                {
                \t"convars"
                \t{
                \t\t"sensitivity"\t\t"1.5"
                \t}
                }
                """);

        saver.save(file, Map.of("cl_crosshairsize", "4"));

        VdfObject convars = VdfParser.parse(file)
                .getObject("UserConvars").orElseThrow()
                .getObject("convars").orElseThrow();
        assertEquals("4", convars.getString("cl_crosshairsize").orElseThrow());
        assertEquals("1.5", convars.getString("sensitivity").orElseThrow());
    }

    @Test
    void reReadsTheFileFreshBeforeSaving() throws IOException {
        Path file = writeConfig("""
                "convars"
                {
                \t"cl_crosshairsize"\t\t"3"
                \t"sensitivity"\t\t"1.5"
                }
                """);
        saver.save(file, Map.of("cl_crosshairsize", "6"));

        VdfObject externallyChanged = VdfParser.parse(file);
        externallyChanged.getObject("convars").orElseThrow().set("sensitivity", "2.0");
        Files.writeString(file, VdfWriter.toText(externallyChanged));

        saver.save(file, Map.of("cl_crosshairsize", "8"));

        VdfObject convars = VdfParser.parse(file).getObject("convars").orElseThrow();
        assertEquals("8", convars.getString("cl_crosshairsize").orElseThrow());
        assertEquals("2.0", convars.getString("sensitivity").orElseThrow());
    }

    @Test
    void throwsAndCreatesNothingWhenTargetMissing() {
        Path missing = tempDir.resolve("missing.vcfg");

        assertThrows(NoSuchFileException.class, () -> saver.save(missing, Map.of("cl_crosshairsize", "3")));

        assertTrue(isEmptyDirectory(tempDir));
    }

    @Test
    void leavesNoTempFileBehind() throws IOException {
        Path file = writeConfig("\"convars\"\n{\n\t\"cl_crosshairsize\"\t\t\"3\"\n}\n");

        saver.save(file, Map.of("cl_crosshairsize", "5"));

        try (var files = Files.list(tempDir)) {
            assertEquals(java.util.List.of(file), files.toList());
        }
    }

    private static boolean isEmptyDirectory(Path dir) {
        try (var files = Files.list(dir)) {
            return files.findAny().isEmpty();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
