package de.bsommerfeld.randomizer.ui.view.viewmodel.settings;

import com.google.inject.Inject;
import de.bsommerfeld.model.action.sequence.ActionSequenceExecutorRunnable;
import de.bsommerfeld.model.config.keybind.KeyBindRepository;
import de.bsommerfeld.randomizer.bootstrap.CS2ConfigLoader;
import de.bsommerfeld.randomizer.config.RandomizerConfig;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GeneralSettingsViewModel {

  @Getter private final LongProperty spentTimeProperty = new SimpleLongProperty();
  @Getter private final StringProperty configPathProperty = new SimpleStringProperty();
  @Getter private final BooleanProperty showIntroProperty = new SimpleBooleanProperty(true);
  @Getter private final IntegerProperty minIntervalProperty = new SimpleIntegerProperty();
  @Getter private final IntegerProperty maxIntervalProperty = new SimpleIntegerProperty();

  private final RandomizerConfig randomizerConfig;
  private final KeyBindRepository keyBindRepository;
  private final CS2ConfigLoader CS2ConfigLoader;

  @Inject
  public GeneralSettingsViewModel(
      RandomizerConfig randomizerConfig, KeyBindRepository keyBindRepository, CS2ConfigLoader CS2ConfigLoader) {
    this.randomizerConfig = randomizerConfig;
    this.keyBindRepository = keyBindRepository;
    this.CS2ConfigLoader = CS2ConfigLoader;
  }

  public CompletionStage<Void> loadConfigs() {
    return CompletableFuture.runAsync(
            () -> {
              String userConfigPath = randomizerConfig.getConfigPath();
              if (randomizerConfig.getConfigPath() == null
                  || randomizerConfig.getConfigPath().isEmpty()) {
                randomizerConfig.setConfigPath(
                    CS2ConfigLoader.ladeUserConfigPath().replace("\\", "/"));
              } else {
                String loadedConfigPath =
                    noThrow(() -> CS2ConfigLoader.ladeUserConfigPath().replace("\\", "/"));
                if(loadedConfigPath != null && !loadedConfigPath.equals(userConfigPath)) {
                  randomizerConfig.setConfigPath(loadedConfigPath); // TODO: what if it always finds the wrong config?
                }
              }
              randomizerConfig.save();
              CS2ConfigLoader.ladeDefaultKeyBinds();
              CS2ConfigLoader.ladeUserKeyBinds();
            })
        .thenRunAsync(
            () -> configPathProperty.set(randomizerConfig.getConfigPath()), Platform::runLater);
  }

  public boolean isThereAnyKeyBinds() {
    return keyBindRepository.hasAnyKeyBinds();
  }

  public void reloadKeyBinds() {
    CS2ConfigLoader.ladeUserKeyBinds();
  }

  private <T> T noThrow(Supplier<T> tSupplier) {
    try {
      return tSupplier.get();
    } catch (Exception e) {
      return null;
    }
  }

  public void setConfigPath(String configPath) {
    randomizerConfig.setConfigPath(configPath);
    randomizerConfig.save();
    configPathProperty.set(configPath);
  }

  public String getCurrentConfigPath() {
    return configPathProperty.get();
  }

  public void setupViewModel() {
    loadIntervalFromConfig();
    spentTimeProperty.set(randomizerConfig.getTimeTracked());
    showIntroProperty.set(randomizerConfig.isShowIntro());
    showIntroProperty.addListener((_, _, _) -> saveRegularSettings());
    minIntervalProperty.addListener((_, _, _) -> updateInterval());
    maxIntervalProperty.addListener((_, _, _) -> updateInterval());
  }

  public String getConfigPath() {
    return randomizerConfig.getConfigPath();
  }

  private void loadIntervalFromConfig() {
    minIntervalProperty.set(randomizerConfig.getMinInterval());
    maxIntervalProperty.set(randomizerConfig.getMaxInterval());
  }

  public void updateInterval() {
    randomizerConfig.setMinInterval(minIntervalProperty.get());
    randomizerConfig.setMaxInterval(maxIntervalProperty.get());
    ActionSequenceExecutorRunnable.setMinWaitTime(minIntervalProperty.get());
    ActionSequenceExecutorRunnable.setMaxWaitTime(maxIntervalProperty.get());
    randomizerConfig.save();
  }

  public void saveRegularSettings() {
    randomizerConfig.setShowIntro(showIntroProperty.get());
    randomizerConfig.save();
  }
}
