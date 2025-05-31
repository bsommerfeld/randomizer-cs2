package de.bsommerfeld.model.config;

import de.bsommerfeld.model.config.keybind.KeyBindRepository;
import java.io.File;
import java.io.FileNotFoundException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConfigLoader {

  private static final String[] STEAM_PATHS = {
    "C:/Program Files (x86)/Steam",
    "C:/Program Files/Steam",
    "C:/Steam",
    "D:/SteamLibrary",
    "D:/Program Files (x86)/Steam",
    "D:/Steam",
    "D:/Games/Steam"
  };

  public static void loadDefaultKeyBinds(String configPath, KeyBindRepository keyBindRepository)
      throws FileNotFoundException {
    File defaultConfigFile = new File(configPath);
    if (!defaultConfigFile.exists()) {
      throw new FileNotFoundException("Standardkonfigurationsdatei nicht gefunden.");
    }
    if (configPath != null) {
      log.info("Lade Standard-Key-Bindings von: {}", configPath);
      keyBindRepository.initDefaults(configPath);
    } else {
      throw new FileNotFoundException("Standardkonfigurationsdatei nicht gefunden.");
    }
  }

  public static void loadUserKeyBindings(String configPath, KeyBindRepository keyBindRepository) {
    File userConfigFile = new File(configPath);
    if (!userConfigFile.exists()) {
      log.info("Keine Benutzer-Key-Bindings gefunden.");
      return;
    }
    if (configPath != null) {
      log.info("Lade Benutzer-Key-Bindings von: {}", configPath);
      keyBindRepository.initModifiedKeyBinds(configPath);
    } else {
      log.info("Keine Benutzer-Key-Bindings gefunden.");
    }
  }

  public static String findDefaultConfigFile() {
    for (String steamPath : STEAM_PATHS) {
      File defaultConfig =
          new File(
              steamPath
                  + "/steamapps/common/Counter-Strike Global Offensive/game/csgo/cfg/user_keys_default.vcfg");
      if (defaultConfig.exists()) {
        return defaultConfig.getAbsolutePath();
      }
    }
    throw new IllegalStateException("Standardkonfigurationsdatei nicht gefunden.");
  }

  public static String findUserConfigFile() {
    for (String steamPath : STEAM_PATHS) {
      File userdataFolder = new File(steamPath + "/userdata");
      if (userdataFolder.exists()) {
        File[] userDirs = listUserDirectories(userdataFolder);
        if (userDirs != null) {
          String configFilePath = searchInUserDirectories(userDirs);
          if (configFilePath != null) {
            return configFilePath;
          }
        }
      }
    }
    throw new IllegalStateException("Keine Benutzerkonfigurationsdatei gefunden.");
  }

  private static File[] listUserDirectories(File userdataFolder) {
    return userdataFolder.listFiles(File::isDirectory);
  }

  private static String searchInUserDirectories(File[] userDirs) {
    for (File userDir : userDirs) {
      File gameFolder = new File(userDir, "730/remote/cs2_user_keys.vcfg");
      if (gameFolder.exists()) {
        return gameFolder.getAbsolutePath();
      }
    }
    return null;
  }
}
