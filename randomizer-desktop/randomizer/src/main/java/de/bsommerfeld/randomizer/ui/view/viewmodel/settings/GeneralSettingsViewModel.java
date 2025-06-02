package de.bsommerfeld.randomizer.ui.view.viewmodel.settings;

import com.google.inject.Inject;
import de.bsommerfeld.model.action.sequence.ActionSequenceExecutorRunnable;
import de.bsommerfeld.model.config.keybind.KeyBindRepository;
import de.bsommerfeld.randomizer.bootstrap.CS2ConfigLoader;
import de.bsommerfeld.randomizer.config.RandomizerConfig;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GeneralSettingsViewModel {

  @Getter private final StringProperty configPathProperty = new SimpleStringProperty();
  @Getter private final BooleanProperty showIntroProperty = new SimpleBooleanProperty(true);
  @Getter private final IntegerProperty minIntervalProperty = new SimpleIntegerProperty();
  @Getter private final IntegerProperty maxIntervalProperty = new SimpleIntegerProperty();

  private final RandomizerConfig randomizerConfig;
  private final KeyBindRepository keyBindRepository;
  private final CS2ConfigLoader CS2ConfigLoader;

  @Inject
  public GeneralSettingsViewModel(
      RandomizerConfig randomizerConfig,
      KeyBindRepository keyBindRepository,
      CS2ConfigLoader CS2ConfigLoader) {
    this.randomizerConfig = randomizerConfig;
    this.keyBindRepository = keyBindRepository;
    this.CS2ConfigLoader = CS2ConfigLoader;
  }

  public CompletionStage<Void> loadConfigs() {
    return CompletableFuture.runAsync(
        () -> {
          try {
            configPathProperty.set(randomizerConfig.getConfigPath());
            randomizerConfig.setConfigPath(CS2ConfigLoader.ladeUserConfigPath().replace("\\", "/"));
            randomizerConfig.save();
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        });
  }

  public boolean isThereAnyKeyBinds() {
    return keyBindRepository.hasAnyKeyBinds();
  }

  public void reloadKeyBinds() {
    CS2ConfigLoader.ladeUserKeyBinds();
  }

  public void setConfigPath(String configPath) {
    configPathProperty.set(configPath);
    randomizerConfig.setConfigPath(configPath);
    randomizerConfig.save();
  }

  public String getCurrentConfigPath() {
    return configPathProperty.get();
  }

  public void setupViewModel() {
    loadIntervalFromConfig();
    configPathProperty.set(randomizerConfig.getConfigPath());
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
