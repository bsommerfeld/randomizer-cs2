package com.revortix.randomizer.bootstrap;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.revortix.model.ApplicationContext;
import com.revortix.model.tracker.TimeTracker;
import com.revortix.randomizer.config.RandomizerConfig;
import com.revortix.randomizer.ui.view.ViewProvider;
import com.revortix.randomizer.ui.view.viewmodel.HomeViewModel;
import com.revortix.randomizer.ui.view.viewmodel.NavigationBarViewModel;
import com.revortix.randomizer.ui.view.viewmodel.RandomizerViewModel;
import com.revortix.randomizer.ui.view.viewmodel.builder.BuilderActionsViewModel;
import com.revortix.randomizer.ui.view.viewmodel.builder.BuilderEditorViewModel;
import com.revortix.randomizer.ui.view.viewmodel.builder.BuilderViewModel;
import com.revortix.randomizer.ui.view.viewmodel.settings.ActionSettingsViewModel;
import com.revortix.randomizer.ui.view.viewmodel.settings.GeneralSettingsViewModel;
import com.revortix.randomizer.ui.view.viewmodel.settings.UpdateSettingsViewModel;
import de.bsommerfeld.jshepherd.core.ConfigurationLoader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class RandomizerModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(NavigationBarViewModel.class).asEagerSingleton();
    bind(BuilderViewModel.class).asEagerSingleton();
    bind(BuilderEditorViewModel.class).asEagerSingleton();
    bind(BuilderActionsViewModel.class).asEagerSingleton();
    bind(RandomizerViewModel.class).asEagerSingleton();
    bind(ActionSettingsViewModel.class).asEagerSingleton();
    bind(UpdateSettingsViewModel.class).asEagerSingleton();
    bind(GeneralSettingsViewModel.class).asEagerSingleton();
    bind(HomeViewModel.class).asEagerSingleton();
    bind(ViewProvider.class).asEagerSingleton();
    bind(RandomizerUpdater.class).asEagerSingleton();
    bind(CS2ConfigLoader.class).asEagerSingleton();
    bind(TimeTracker.class).asEagerSingleton();
  }

  @Provides
  @Singleton
  RandomizerConfig provideRandomizerConfig() {
    Path configFile =
        Paths.get(ApplicationContext.getAppdataFolder().getPath(), "randomizer-config.yaml");

    try {
      if (configFile.getParent() != null) {
        Files.createDirectories(configFile.getParent());
      }
    } catch (IOException e) {
      System.err.println("Could not create config directory: " + e.getMessage());
      throw new RuntimeException("Failed to create config directory", e);
    }

    return ConfigurationLoader.load(
        configFile, RandomizerConfig.class, RandomizerConfig::new, true);
  }
}
