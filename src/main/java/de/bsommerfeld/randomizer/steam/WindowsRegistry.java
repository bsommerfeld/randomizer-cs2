package de.bsommerfeld.randomizer.steam;

import java.util.Optional;

/** Read access to the Windows registry, abstracted as an interface for testability. */
public interface WindowsRegistry {

    enum Hive {
        CURRENT_USER,
        LOCAL_MACHINE
    }

    Optional<String> readString(Hive hive, String keyPath, String valueName);
}
