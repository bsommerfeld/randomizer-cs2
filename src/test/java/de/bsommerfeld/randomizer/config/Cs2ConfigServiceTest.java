package de.bsommerfeld.randomizer.config;

import de.bsommerfeld.randomizer.steam.SteamLocator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Cs2ConfigServiceTest {

    @TempDir
    Path tempDir;

    private static SteamLocator locatorFindingNothing() {
        return new SteamLocator((hive, keyPath, valueName) -> Optional.empty());
    }

    private Path fixtureConfig() throws Exception {
        return Path.of(getClass().getResource("/fixtures/user_keys_default.vcfg").toURI());
    }

    @Test
    void convertsVcfgToPrettyJson() throws Exception {
        Cs2ConfigService service = new Cs2ConfigService(new AppPreferences(tempDir), locatorFindingNothing());

        Cs2ConfigService.LoadedConfig loaded = service.load(fixtureConfig());

        assertTrue(loaded.prettyJson().contains("\"config\""));
        assertTrue(loaded.prettyJson().contains("\"bindings\""));
        assertTrue(loaded.prettyJson().contains("\"w\": \"+forward\""));
    }

    @Test
    void displaysBackslashKeyUnescaped() throws Exception {
        Cs2ConfigService service = new Cs2ConfigService(new AppPreferences(tempDir), locatorFindingNothing());
        Path config = tempDir.resolve("cs2_user_keys.vcfg");
        java.nio.file.Files.writeString(config, "\"bindings\"\n{\n\t\"\\\"\t\t\"toggleconsole\"\n}");

        Cs2ConfigService.LoadedConfig loaded = service.load(config);

        // The backslash key is displayed unescaped: "\" instead of "\\"
        assertTrue(loaded.prettyJson().contains("\"\\\": \"toggleconsole\""));
    }

    @Test
    void loadAndRememberPersistsPathPerKind() throws Exception {
        AppPreferences preferences = new AppPreferences(tempDir);
        Cs2ConfigService service = new Cs2ConfigService(preferences, locatorFindingNothing());
        Path config = fixtureConfig();

        service.loadAndRemember(ConfigKind.USER, config);

        assertEquals(Optional.of(config), preferences.getConfigPathOverride(ConfigKind.USER));
        assertTrue(preferences.getConfigPathOverride(ConfigKind.DEFAULT).isEmpty());
    }

    @Test
    void startupUsesRememberedPath() throws Exception {
        AppPreferences preferences = new AppPreferences(tempDir);
        Path config = fixtureConfig();
        preferences.setConfigPathOverride(ConfigKind.DEFAULT, config);
        Cs2ConfigService service = new Cs2ConfigService(preferences, locatorFindingNothing());

        Optional<Cs2ConfigService.LoadedConfig> loaded = service.loadConfigOnStartup(ConfigKind.DEFAULT);

        assertEquals(Optional.of(config), loaded.map(Cs2ConfigService.LoadedConfig::source));
    }

    @Test
    void startupIgnoresRememberedPathWhenFileIsGone() throws Exception {
        AppPreferences preferences = new AppPreferences(tempDir);
        preferences.setConfigPathOverride(ConfigKind.DEFAULT, tempDir.resolve("missing.vcfg"));
        Cs2ConfigService service = new Cs2ConfigService(preferences, locatorFindingNothing());

        assertTrue(service.loadConfigOnStartup(ConfigKind.DEFAULT).isEmpty());
    }

    @Test
    void redetectIgnoresRememberedPath() throws Exception {
        AppPreferences preferences = new AppPreferences(tempDir);
        preferences.setConfigPathOverride(ConfigKind.DEFAULT, fixtureConfig());
        Cs2ConfigService service = new Cs2ConfigService(preferences, locatorFindingNothing());

        // Startup would use the remembered path, re-detection must not
        assertTrue(service.loadConfigOnStartup(ConfigKind.DEFAULT).isPresent());
        assertTrue(service.redetectAndLoad(ConfigKind.DEFAULT).isEmpty());
    }

    @Test
    void startupReturnsEmptyForUserConfigWhenNothingIsFound() {
        Cs2ConfigService service = new Cs2ConfigService(new AppPreferences(tempDir), locatorFindingNothing());

        assertTrue(service.loadConfigOnStartup(ConfigKind.USER).isEmpty());
    }
}
