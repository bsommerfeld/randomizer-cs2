package de.bsommerfeld.randomizer.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigVerifierTest {

    @TempDir
    Path tempDir;

    private final ConfigVerifier verifier = new ConfigVerifier();

    private Path write(String content) throws IOException {
        Path file = tempDir.resolve("cs2_user_convars.vcfg");
        Files.writeString(file, content);
        return file;
    }

    @Test
    void allValuesPresentReportsNothing() throws IOException {
        Path file = write("""
                "user_convars"
                {
                    "convars"
                    {
                        "cl_crosshairsize" "3"
                        "cl_crosshairdot" "1"
                    }
                }
                """);

        assertTrue(verifier.missingValues(file,
                Map.of("cl_crosshairsize", "3", "cl_crosshairdot", "1")).isEmpty());
    }

    @Test
    void missingKeyIsReported() throws IOException {
        Path file = write("""
                "user_convars"
                {
                    "convars"
                    {
                        "cl_crosshairsize" "3"
                    }
                }
                """);

        assertEquals(Map.of("cl_crosshairdot", "1"),
                verifier.missingValues(file, Map.of("cl_crosshairdot", "1")));
    }

    @Test
    void keyWithDifferentValueIsReported() throws IOException {
        Path file = write("""
                "user_convars"
                {
                    "convars"
                    {
                        "cl_crosshairsize" "5"
                    }
                }
                """);

        assertEquals(Map.of("cl_crosshairsize", "3"),
                verifier.missingValues(file, Map.of("cl_crosshairsize", "3")));
    }

    @Test
    void staleDuplicateOccurrenceIsReported() throws IOException {
        // ConfigSaver replaces every occurrence, so one leftover old value means the save did not take.
        Path file = write("""
                "user_convars"
                {
                    "convars"
                    {
                        "cl_crosshairsize" "3"
                    }
                    "other"
                    {
                        "cl_crosshairsize" "5"
                    }
                }
                """);

        assertEquals(Map.of("cl_crosshairsize", "3"),
                verifier.missingValues(file, Map.of("cl_crosshairsize", "3")));
    }
}
