package de.bsommerfeld.randomizer.game;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcessGameDetectorTest {

    @Test
    void matchesWindowsExecutablePath() {
        assertTrue(ProcessGameDetector.isCs2Executable(
                "C:\\Program Files (x86)\\Steam\\steamapps\\common\\Counter-Strike Global Offensive"
                        + "\\game\\bin\\win64\\cs2.exe"));
    }

    @Test
    void matchesCaseInsensitively() {
        assertTrue(ProcessGameDetector.isCs2Executable("C:\\Games\\CS2.EXE"));
    }

    @Test
    void matchesLinuxExecutablePath() {
        assertTrue(ProcessGameDetector.isCs2Executable(
                "/home/user/.steam/steamapps/common/Counter-Strike Global Offensive"
                        + "/game/bin/linuxsteamrt64/cs2"));
    }

    @Test
    void rejectsOtherProcesses() {
        assertFalse(ProcessGameDetector.isCs2Executable("C:\\Windows\\System32\\svchost.exe"));
        assertFalse(ProcessGameDetector.isCs2Executable("C:\\Steam\\cs2_helper.exe"));
        // Only the file name counts, not a folder called cs2 somewhere in the path
        assertFalse(ProcessGameDetector.isCs2Executable("C:\\Games\\cs2\\launcher.exe"));
        assertFalse(ProcessGameDetector.isCs2Executable(""));
    }

    @Test
    void processScanDoesNotThrow() {
        assertDoesNotThrow(ProcessGameDetector::isCs2Running);
    }
}
