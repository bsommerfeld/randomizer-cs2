package de.bsommerfeld.randomizer.steam;

import de.bsommerfeld.randomizer.vdf.VdfObject;
import de.bsommerfeld.randomizer.vdf.VdfParser;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Locates the CS2 configs: reads the Steam install path from the Windows registry,
 * collects all Steam libraries from {@code steamapps/libraryfolders.vdf} and searches
 * them for CS2 (app id 730).
 */
public final class SteamLocator {

    private static final String CS2_CONFIG_RELATIVE = "steamapps/common/Counter-Strike Global Offensive/game/csgo/cfg/user_keys_default.vcfg";
    private static final String CS2_APP_MANIFEST = "steamapps/appmanifest_730.acf";
    private static final String CS2_USER_KEYS_RELATIVE = "730/remote/cs2_user_keys.vcfg";
    private static final String CS2_USER_CONVARS_RELATIVE = "730/remote/cs2_user_convars.vcfg";

    private final WindowsRegistry registry;

    public SteamLocator(WindowsRegistry registry) {
        this.registry = registry;
    }

    /** Returns the path to user_keys_default.vcfg or empty; never throws. */
    public Optional<Path> findUserKeysDefaultConfig() {
        return findSteamRoot().flatMap(this::findConfigInLibraries);
    }

    /**
     * Returns the user's custom keybinds ({@code userdata/<SteamID>/730/remote/cs2_user_keys.vcfg})
     * or empty; never throws. With multiple Steam accounts the most recently modified file wins.
     */
    public Optional<Path> findUserKeysConfig() {
        return findSteamRoot().flatMap(root -> findInUserdata(root, CS2_USER_KEYS_RELATIVE));
    }

    /**
     * Returns the user's convars ({@code userdata/<SteamID>/730/remote/cs2_user_convars.vcfg}),
     * which holds the crosshair settings, or empty; never throws. With multiple Steam accounts the
     * most recently modified file wins.
     */
    public Optional<Path> findUserConvarsConfig() {
        return findSteamRoot().flatMap(root -> findInUserdata(root, CS2_USER_CONVARS_RELATIVE));
    }

    private static Optional<Path> findInUserdata(Path steamRoot, String relative) {
        Path userdata = steamRoot.resolve("userdata");
        if (!Files.isDirectory(userdata)) {
            return Optional.empty();
        }
        List<Path> candidates = new ArrayList<>();
        try (DirectoryStream<Path> userDirs = Files.newDirectoryStream(userdata)) {
            for (Path userDir : userDirs) {
                if (!isNumeric(userDir.getFileName().toString())) {
                    continue;
                }
                Path config = userDir.resolve(relative);
                if (Files.isRegularFile(config)) {
                    candidates.add(config);
                }
            }
        } catch (IOException e) {
            return Optional.empty();
        }
        return candidates.stream().max(Comparator.comparing(SteamLocator::lastModified));
    }

    private static FileTime lastModified(Path file) {
        try {
            return Files.getLastModifiedTime(file);
        } catch (IOException e) {
            return FileTime.fromMillis(0);
        }
    }

    private Optional<Path> findSteamRoot() {
        return registry.readString(WindowsRegistry.Hive.CURRENT_USER, "Software\\Valve\\Steam", "SteamPath")
                .or(() -> registry.readString(
                        WindowsRegistry.Hive.LOCAL_MACHINE, "SOFTWARE\\WOW6432Node\\Valve\\Steam", "InstallPath"))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .flatMap(SteamLocator::toDirectory);
    }

    private Optional<Path> findConfigInLibraries(Path steamRoot) {
        List<Path> libraries = findLibraries(steamRoot);
        // Prefer the library where CS2 is actually installed according to its app manifest
        for (Path library : libraries) {
            if (Files.isRegularFile(library.resolve(CS2_APP_MANIFEST))) {
                Path config = library.resolve(CS2_CONFIG_RELATIVE);
                if (Files.isRegularFile(config)) {
                    return Optional.of(config);
                }
            }
        }
        for (Path library : libraries) {
            Path config = library.resolve(CS2_CONFIG_RELATIVE);
            if (Files.isRegularFile(config)) {
                return Optional.of(config);
            }
        }
        return Optional.empty();
    }

    private List<Path> findLibraries(Path steamRoot) {
        Set<Path> libraries = new LinkedHashSet<>();
        libraries.add(steamRoot);
        Path libraryFoldersVdf = steamRoot.resolve("steamapps/libraryfolders.vdf");
        if (Files.isRegularFile(libraryFoldersVdf)) {
            try {
                collectLibraries(VdfParser.parse(libraryFoldersVdf), libraries);
            } catch (IOException | RuntimeException e) {
                // Unreadable or broken libraryfolders.vdf: continue with the Steam root only
            }
        }
        return List.copyOf(libraries);
    }

    private static void collectLibraries(VdfObject document, Set<Path> libraries) {
        // The top-level key is "libraryfolders" (modern) or "LibraryFolders" (legacy)
        for (Object value : document.entries().values()) {
            if (!(value instanceof VdfObject folders)) {
                continue;
            }
            folders.entries().forEach((key, entry) -> {
                if (!isNumeric(key)) {
                    return;
                }
                String path = switch (entry) {
                    case String direct -> direct; // legacy: "1" "D:\\SteamLibrary"
                    case VdfObject folder -> folder.getString("path").orElse(null); // modern: "1" { "path" ... }
                    default -> null;
                };
                if (path != null) {
                    try {
                        libraries.add(Path.of(path));
                    } catch (InvalidPathException ignored) {
                    }
                }
            });
        }
    }

    private static boolean isNumeric(String value) {
        return !value.isEmpty() && value.chars().allMatch(Character::isDigit);
    }

    private static Optional<Path> toDirectory(String value) {
        try {
            Path path = Path.of(value);
            return Files.isDirectory(path) ? Optional.of(path) : Optional.empty();
        } catch (InvalidPathException e) {
            return Optional.empty();
        }
    }
}
