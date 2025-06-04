package de.bsommerfeld.randomizer.bootstrap;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.google.inject.Inject;
import de.bsommerfeld.model.action.Action;
import de.bsommerfeld.model.action.ActionKey;
import de.bsommerfeld.model.action.config.ActionConfig;
import de.bsommerfeld.model.action.impl.BaseAction;
import de.bsommerfeld.model.action.impl.MouseMoveAction;
import de.bsommerfeld.model.action.impl.PauseAction;
import de.bsommerfeld.model.action.spi.ActionExecutor;
import de.bsommerfeld.model.action.spi.ActionRepository;
import de.bsommerfeld.model.action.spi.ActionSequenceExecutor;
import de.bsommerfeld.model.action.spi.ActionSequenceRepository;
import de.bsommerfeld.model.action.spi.FocusManager;
import de.bsommerfeld.model.config.keybind.KeyBindRepository;
import de.bsommerfeld.model.exception.UncaughtExceptionLogger;
import de.bsommerfeld.model.messages.Messages;
import de.bsommerfeld.model.watcher.FileSystemWatcher;
import de.bsommerfeld.randomizer.config.RandomizerConfig;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * The RandomizerBootstrap class is responsible for initializing and configuring the application. It
 * sets up repositories, executors, file watchers, exception handlers, and loads necessary
 * configurations.
 */
@Slf4j
public class RandomizerBootstrap {

  @Getter private final ActionSequenceRepository actionSequenceRepository;
  @Getter private final ActionRepository actionRepository;
  private final KeyBindRepository keyBindRepository;
  private final ActionSequenceExecutor actionSequenceExecutor;
  private final ActionExecutor actionExecutor;
  private final FocusManager focusManager;
  private final ActionConfig actionConfig;
  private final RandomizerConfig randomizerConfig;
  private final CS2ConfigLoader CS2ConfigLoader;

  @Inject
  public RandomizerBootstrap(
      ActionSequenceRepository actionSequenceRepository,
      ActionRepository actionRepository,
      KeyBindRepository keyBindRepository,
      ActionSequenceExecutor actionSequenceExecutor,
      ActionExecutor actionExecutor,
      FocusManager focusManager,
      ActionConfig actionConfig,
      RandomizerConfig randomizerConfig,
      CS2ConfigLoader CS2ConfigLoader) {
    this.actionSequenceRepository = actionSequenceRepository;
    this.actionRepository = actionRepository;
    this.keyBindRepository = keyBindRepository;
    this.actionSequenceExecutor = actionSequenceExecutor;
    this.actionExecutor = actionExecutor;
    this.focusManager = focusManager;
    this.actionConfig = actionConfig;
    this.randomizerConfig = randomizerConfig;
    this.CS2ConfigLoader = CS2ConfigLoader;
  }

  public void initializeApplication() {
    log.info("Initializing application...");
    loadConfiguration();
    loadUserKeyBindsByConfig();
    initializeActionDependencies();
    registerActions();
    setupFileWatcher();
    Messages.cache();
    setupGlobalExceptionHandler();
    registerNativeKeyHook();
    cacheActionSequences();
    startExecutor();
  }

  private void initializeActionDependencies() {
    log.info("Initializing action dependencies...");
    Action.setDependencies(actionExecutor, focusManager, actionConfig);
  }

  private void loadUserKeyBindsByConfig() {
    try {
      if (randomizerConfig.getConfigPath() != null && !randomizerConfig.getConfigPath().isEmpty()) {
        CS2ConfigLoader.ladeDefaultKeyBinds();
        CS2ConfigLoader.ladeUserKeyBinds();
      }
    } catch (Exception e) {
      log.error("Error loading user keybinds", e);
    }
  }

  private void loadConfiguration() {
    log.info("Loading configuration...");

    actionSequenceExecutor.setMinWaitTime(randomizerConfig.getMinInterval());
    actionSequenceExecutor.setMaxWaitTime(randomizerConfig.getMaxInterval());
  }

  private void setupFileWatcher() {
    log.info("Starting FileWatcher");
    FileSystemWatcher fileSystemWatcher = new FileSystemWatcher();
    fileSystemWatcher.addFileChangeListener(_ -> cacheActionSequences());
    startThread(new Thread(fileSystemWatcher));
  }

  private void setupGlobalExceptionHandler() {
    log.info("Setting up Global Exception Handler...");
    Thread.currentThread()
        .setUncaughtExceptionHandler(UncaughtExceptionLogger.DEFAULT_UNCAUGHT_EXCEPTION_LOGGER);
  }

  private void cacheActionSequences() {
    log.info("Caching ActionSequences...");
    actionSequenceRepository.updateActionSequencesCache();
  }

  private void startExecutor() {
    log.info("Starting Executor...");
    Thread executorThread = actionSequenceExecutor.start();
    executorThread.setUncaughtExceptionHandler(UncaughtExceptionLogger.DEFAULT_UNCAUGHT_EXCEPTION_LOGGER);
  }

  private void startThread(Thread thread) {
    log.info("Starting Thread: {}", thread.getName());
    thread.setUncaughtExceptionHandler(UncaughtExceptionLogger.DEFAULT_UNCAUGHT_EXCEPTION_LOGGER);
    thread.setDaemon(true);
    thread.start();
  }

  private void registerActions() {
    log.info("Registering Actions...");
    actionRepository.register(new PauseAction());
    actionRepository.register(new MouseMoveAction());
    // actionRepository.register(new BaseAction("Escape", ActionKey.of("ESCAPE")));
    registerKeyBindActions();
    log.info("{} Actions registered", actionRepository.getActions().size());
  }

  private void registerKeyBindActions() {
    keyBindRepository
        .getKeyBinds()
        .forEach(
            keyBind -> {
              Action action = new BaseAction(keyBind.getAction(), ActionKey.of(keyBind.getKey()));
              if (!actionRepository.hasActionWithName(action.getName())) {
                actionRepository.register(action);
              } else {
                log.debug("Action {} already exists", action.getName());
              }
            });
  }

  private void registerNativeKeyHook() {
    try {
      log.info("Registering Native Key Hook...");
      GlobalScreen.registerNativeHook();
    } catch (NativeHookException e) {
      log.error("Error registering Native Hook", e);
    }
  }
}
