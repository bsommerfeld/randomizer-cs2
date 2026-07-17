package de.bsommerfeld.randomizer.steam;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SteamLocatorTest {

    private static final String CS2_CONFIG_RELATIVE =
            "steamapps/common/Counter-Strike Global Offensive/game/csgo/cfg/user_keys_default.vcfg";

    @TempDir
    Path tempDir;

    private static WindowsRegistry hkcuRegistry(Path steamRoot) {
        return (hive, keyPath, valueName) ->
                hive == WindowsRegistry.Hive.CURRENT_USER && "SteamPath".equals(valueName)
                        ? Optional.of(steamRoot.toString())
                        : Optional.empty();
    }

    private static String vdfEscape(Path path) {
        return path.toString().replace("\\", "\\\\");
    }

    private Path createCs2Config(Path library) throws IOException {
        Path config = library.resolve(CS2_CONFIG_RELATIVE);
        Files.createDirectories(config.getParent());
        Files.writeString(config, "\"config\"\n{\n}\n");
        return config;
    }

    private Path createSteamRoot() throws IOException {
        Path steamRoot = tempDir.resolve("Steam");
        Files.createDirectories(steamRoot.resolve("steamapps"));
        return steamRoot;
    }

    @Test
    void findsConfigInSecondaryLibraryViaModernVdf() throws IOException {
        Path steamRoot = createSteamRoot();
        Path library = tempDir.resolve("SteamLibrary");
        Path config = createCs2Config(library);
        Files.createDirectories(library.resolve("steamapps"));
        Files.writeString(library.resolve("steamapps/appmanifest_730.acf"),
                "\"AppState\"\n{\n\t\"appid\"\t\t\"730\"\n}\n");
        Files.writeString(steamRoot.resolve("steamapps/libraryfolders.vdf"), """
                "libraryfolders"
                {
                    "0"
                    {
                        "path"    "%s"
                    }
                    "1"
                    {
                        "path"    "%s"
                        "apps"
                        {
                            "730"    "123456789"
                        }
                    }
                }
                """.formatted(vdfEscape(steamRoot), vdfEscape(library)));

        SteamLocator locator = new SteamLocator(hkcuRegistry(steamRoot));

        assertEquals(Optional.of(config), locator.findUserKeysDefaultConfig());
    }

    @Test
    void findsConfigViaLegacyVdfWithoutManifest() throws IOException {
        Path steamRoot = createSteamRoot();
        Path library = tempDir.resolve("SteamLibrary");
        Path config = createCs2Config(library);
        Files.writeString(steamRoot.resolve("steamapps/libraryfolders.vdf"), """
                "LibraryFolders"
                {
                    "TimeNextStatsReport"    "1234567890"
                    "ContentStatsID"    "-123456"
                    "1"    "%s"
                }
                """.formatted(vdfEscape(library)));

        SteamLocator locator = new SteamLocator(hkcuRegistry(steamRoot));

        assertEquals(Optional.of(config), locator.findUserKeysDefaultConfig());
    }

    @Test
    void findsConfigInSteamRootWithoutLibraryFoldersVdf() throws IOException {
        Path steamRoot = createSteamRoot();
        Path config = createCs2Config(steamRoot);

        SteamLocator locator = new SteamLocator(hkcuRegistry(steamRoot));

        assertEquals(Optional.of(config), locator.findUserKeysDefaultConfig());
    }

    @Test
    void fallsBackToLocalMachineInstallPath() throws IOException {
        Path steamRoot = createSteamRoot();
        Path config = createCs2Config(steamRoot);
        WindowsRegistry registry = (hive, keyPath, valueName) ->
                hive == WindowsRegistry.Hive.LOCAL_MACHINE && "InstallPath".equals(valueName)
                        ? Optional.of(steamRoot.toString())
                        : Optional.empty();

        SteamLocator locator = new SteamLocator(registry);

        assertEquals(Optional.of(config), locator.findUserKeysDefaultConfig());
    }

    @Test
    void returnsEmptyWhenRegistryHasNoSteam() {
        SteamLocator locator = new SteamLocator((hive, keyPath, valueName) -> Optional.empty());

        assertTrue(locator.findUserKeysDefaultConfig().isEmpty());
    }

    @Test
    void returnsEmptyWhenConfigDoesNotExist() throws IOException {
        Path steamRoot = createSteamRoot();

        SteamLocator locator = new SteamLocator(hkcuRegistry(steamRoot));

        assertTrue(locator.findUserKeysDefaultConfig().isEmpty());
    }

    @Test
    void findsCs2CfgFolderUnderGameInstall() throws IOException {
        Path steamRoot = createSteamRoot();
        createCs2Config(steamRoot); // creates the cfg folder with a file inside

        SteamLocator locator = new SteamLocator(hkcuRegistry(steamRoot));

        assertEquals(Optional.of(steamRoot.resolve(
                        "steamapps/common/Counter-Strike Global Offensive/game/csgo/cfg")),
                locator.findCs2CfgFolder());
    }

    @Test
    void returnsEmptyWhenCfgFolderDoesNotExist() throws IOException {
        Path steamRoot = createSteamRoot();

        SteamLocator locator = new SteamLocator(hkcuRegistry(steamRoot));

        assertTrue(locator.findCs2CfgFolder().isEmpty());
    }

    private Path createUserKeysConfig(Path steamRoot, String steamId, String content) throws IOException {
        Path config = steamRoot.resolve("userdata").resolve(steamId).resolve("730/remote/cs2_user_keys.vcfg");
        Files.createDirectories(config.getParent());
        Files.writeString(config, content);
        return config;
    }

    @Test
    void findsUserKeysConfigInUserdata() throws IOException {
        Path steamRoot = createSteamRoot();
        Path config = createUserKeysConfig(steamRoot, "1055247857", "\"config\"\n{\n}\n");

        SteamLocator locator = new SteamLocator(hkcuRegistry(steamRoot));

        assertEquals(Optional.of(config), locator.findUserKeysConfig());
    }

    @Test
    void picksMostRecentlyModifiedUserKeysConfigAcrossAccounts() throws IOException {
        Path steamRoot = createSteamRoot();
        Path older = createUserKeysConfig(steamRoot, "1055247857", "\"config\"\n{\n}\n");
        Path newer = createUserKeysConfig(steamRoot, "1415487359", "\"config\"\n{\n}\n");
        Files.setLastModifiedTime(older, java.nio.file.attribute.FileTime.fromMillis(1_000_000));
        Files.setLastModifiedTime(newer, java.nio.file.attribute.FileTime.fromMillis(2_000_000));

        SteamLocator locator = new SteamLocator(hkcuRegistry(steamRoot));

        assertEquals(Optional.of(newer), locator.findUserKeysConfig());
    }

    @Test
    void ignoresNonNumericUserdataDirectories() throws IOException {
        Path steamRoot = createSteamRoot();
        Path config = steamRoot.resolve("userdata/ac/730/remote/cs2_user_keys.vcfg");
        Files.createDirectories(config.getParent());
        Files.writeString(config, "\"config\"\n{\n}\n");

        SteamLocator locator = new SteamLocator(hkcuRegistry(steamRoot));

        assertTrue(locator.findUserKeysConfig().isEmpty());
    }

    @Test
    void returnsEmptyWhenUserdataDoesNotExist() throws IOException {
        Path steamRoot = createSteamRoot();

        SteamLocator locator = new SteamLocator(hkcuRegistry(steamRoot));

        assertTrue(locator.findUserKeysConfig().isEmpty());
    }

    @Test
    void brokenLibraryFoldersVdfStillFindsConfigInSteamRoot() throws IOException {
        Path steamRoot = createSteamRoot();
        Path config = createCs2Config(steamRoot);
        Files.writeString(steamRoot.resolve("steamapps/libraryfolders.vdf"), "\"libraryfolders\" {");

        SteamLocator locator = new SteamLocator(hkcuRegistry(steamRoot));

        assertEquals(Optional.of(config), locator.findUserKeysDefaultConfig());
    }
}
