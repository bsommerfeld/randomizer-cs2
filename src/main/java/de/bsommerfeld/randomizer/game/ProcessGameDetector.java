package de.bsommerfeld.randomizer.game;

import java.util.Locale;
import java.util.Optional;

/**
 * Detects a running CS2 by scanning the process list for {@code cs2.exe} on Windows or {@code cs2}
 * on Linux. {@link ProcessHandle} shows the command path only for the current user's processes.
 * Steam starts the game as that user, so no admin rights are needed.
 */
public final class ProcessGameDetector {

    private ProcessGameDetector() {
    }

    /** Whether the CS2 game client runs on this machine. Starting GSI asks this first. */
    public static boolean isCs2Running() {
        return ProcessHandle.allProcesses()
                .map(process -> process.info().command())
                .flatMap(Optional::stream)
                .anyMatch(ProcessGameDetector::isCs2Executable);
    }

    /** Whether the given full command path names the CS2 game executable. */
    public static boolean isCs2Executable(String command) {
        int lastSeparator = Math.max(command.lastIndexOf('\\'), command.lastIndexOf('/'));
        String fileName = command.substring(lastSeparator + 1).toLowerCase(Locale.ROOT);
        return fileName.equals("cs2.exe") || fileName.equals("cs2");
    }
}
