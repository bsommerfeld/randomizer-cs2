package de.bsommerfeld.randomizer.steam;

import de.bsommerfeld.randomizer.vdf.VdfObject;
import de.bsommerfeld.randomizer.vdf.VdfParser;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Locates the CS2 configs: reads the Steam install path from the Windows registry,
 * collects all Steam libraries from {@code steamapps/libraryfolders.vdf} and searches
 * them for CS2 (app id 730).
 */
public final class SteamLocator {

    private static final String CS2_CONFIG_RELATIVE = "steamapps/common/Counter-Strike Global Offensive/game/csgo/cfg/user_keys_default.vcfg";
    private static final String CS2_APP_MANIFEST = "steamapps/appmanifest_730.acf";
    private static final String CS2_USER_KEYS_RELATIVE = "730/remote/cs2_user_keys.vcfg";

    private final WindowsRegistry registry;

    public SteamLocator(WindowsRegistry registry) {
        this.registry = registry;
    }

    /** Returns the path to user_keys_default.vcfg or empty; never throws. */
    public Optional<Path> findUserKeysDefaultConfig() {
        return findSteamRoot().flatMap(SteamLocator::defaultConfigInLibraries);
    }

    /**
     * Returns the user's custom keybinds ({@code userdata/<SteamID>/730/remote/cs2_user_keys.vcfg})
     * or empty; never throws. With multiple Steam accounts the most recently modified file wins.
     */
    public Optional<Path> findUserKeysConfig() {
        return findSteamRoot()
                .map(root -> userKeysConfigs(root.resolve("userdata")))
                .flatMap(configs -> configs.stream().max(Comparator.comparing(SteamLocator::lastModified)));
    }

    private Optional<Path> findSteamRoot() {
        return registry.readString(WindowsRegistry.Hive.CURRENT_USER, "Software\\Valve\\Steam", "SteamPath")
                .or(() -> registry.readString(
                        WindowsRegistry.Hive.LOCAL_MACHINE, "SOFTWARE\\WOW6432Node\\Valve\\Steam", "InstallPath"))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .flatMap(SteamLocator::toPath)
                .filter(Files::isDirectory);
    }

    /** Prefers the library where CS2 is installed according to its app manifest, then any library. */
    private static Optional<Path> defaultConfigInLibraries(Path steamRoot) {
        List<Path> libraries = findLibraries(steamRoot);
        return firstDefaultConfig(libraries.stream().filter(SteamLocator::hasCs2Manifest))
                .or(() -> firstDefaultConfig(libraries.stream()));
    }

    private static Optional<Path> firstDefaultConfig(Stream<Path> libraries) {
        return libraries.map(library -> library.resolve(CS2_CONFIG_RELATIVE))
                .filter(Files::isRegularFile)
                .findFirst();
    }

    private static boolean hasCs2Manifest(Path library) {
        return Files.isRegularFile(library.resolve(CS2_APP_MANIFEST));
    }

    /** The keybind file of every account folder ({@code userdata/<numeric id>}) that has one. */
    private static List<Path> userKeysConfigs(Path userdata) {
        if (!Files.isDirectory(userdata)) {
            return List.of();
        }
        try (Stream<Path> accounts = Files.list(userdata)) {
            return accounts
                    .filter(account -> isNumeric(account.getFileName().toString()))
                    .map(account -> account.resolve(CS2_USER_KEYS_RELATIVE))
                    .filter(Files::isRegularFile)
                    .toList();
        } catch (IOException | UncheckedIOException e) {
            return List.of();
        }
    }

    private static FileTime lastModified(Path file) {
        try {
            return Files.getLastModifiedTime(file);
        } catch (IOException e) {
            return FileTime.fromMillis(0);
        }
    }

    /** The Steam root plus every library {@code libraryfolders.vdf} lists, without duplicates. */
    private static List<Path> findLibraries(Path steamRoot) {
        Set<Path> libraries = new LinkedHashSet<>();
        libraries.add(steamRoot);
        libraries.addAll(librariesListedIn(steamRoot.resolve("steamapps/libraryfolders.vdf")));
        return List.copyOf(libraries);
    }

    /** A missing, unreadable or broken file lists nothing; the Steam root still counts. */
    private static List<Path> librariesListedIn(Path libraryFoldersVdf) {
        if (!Files.isRegularFile(libraryFoldersVdf)) {
            return List.of();
        }
        try {
            return libraryPaths(VdfParser.parse(libraryFoldersVdf));
        } catch (IOException | RuntimeException e) {
            return List.of();
        }
    }

    private static List<Path> libraryPaths(VdfObject document) {
        // The top-level key is "libraryfolders" (modern) or "LibraryFolders" (legacy)
        return document.entries().values().stream()
                .filter(VdfObject.class::isInstance)
                .map(VdfObject.class::cast)
                .flatMap(folders -> folders.entries().entrySet().stream())
                .filter(entry -> isNumeric(entry.getKey()))
                .flatMap(entry -> libraryPath(entry.getValue()).stream())
                .toList();
    }

    /** The folder one numbered entry names, in either format Steam has used. */
    private static Optional<Path> libraryPath(Object entry) {
        String path = switch (entry) {
            case String direct -> direct; // legacy: "1" "D:\\SteamLibrary"
            case VdfObject folder -> folder.getString("path").orElse(null); // modern: "1" { "path" ... }
            default -> null;
        };
        return Optional.ofNullable(path).flatMap(SteamLocator::toPath);
    }

    private static boolean isNumeric(String value) {
        return !value.isEmpty() && value.chars().allMatch(Character::isDigit);
    }

    private static Optional<Path> toPath(String value) {
        try {
            return Optional.of(Path.of(value));
        } catch (InvalidPathException e) {
            return Optional.empty();
        }
    }
}
