package de.bsommerfeld.model.action.sequence;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import com.github.kwhat.jnativehook.mouse.NativeMouseEvent;
import com.github.kwhat.jnativehook.mouse.NativeMouseListener;
import com.google.inject.Inject;
import de.bsommerfeld.model.ApplicationContext;
import de.bsommerfeld.model.ApplicationState;
import de.bsommerfeld.model.action.Action;
import de.bsommerfeld.model.action.config.ActionConfig;
import de.bsommerfeld.model.action.spi.ActionSequenceDispatcher;
import de.bsommerfeld.model.action.spi.ActionSequenceExecutor;
import de.bsommerfeld.model.action.spi.ActionSequenceRepository;
import de.bsommerfeld.model.action.spi.FocusManager;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import lombok.extern.slf4j.Slf4j;

/**
 * Default implementation of the ActionSequenceExecutor interface. This class executes action
 * sequences based on certain conditions and events.
 */
@Slf4j
public class DefaultActionSequenceExecutor implements ActionSequenceExecutor {

  private final ActionSequenceRepository actionSequenceRepository;
  private final ApplicationContext applicationContext;
  private final ActionSequenceDispatcher actionSequenceDispatcher;
  private final FocusManager focusManager;
  private final ActionConfig actionConfig;

  private long lastFocusCheckTime = 0;
  private volatile ActionSequence currentActionSequence;
  private volatile long lastCycle;
  private volatile int lastWaitTime;
  private volatile boolean hasReleasedAnyKey = false;
  private volatile boolean running = false;
  private Thread executorThread;

  @Inject
  public DefaultActionSequenceExecutor(
      ActionSequenceRepository actionSequenceRepository,
      ApplicationContext applicationContext,
      ActionSequenceDispatcher actionSequenceDispatcher,
      FocusManager focusManager,
      ActionConfig actionConfig) {
    this.actionSequenceRepository = actionSequenceRepository;
    this.applicationContext = applicationContext;
    this.actionSequenceDispatcher = actionSequenceDispatcher;
    this.focusManager = focusManager;
    this.actionConfig = actionConfig;
    registerNativeHookListenerForEachKeyBind();
    registerApplicationStateChangeListener();
  }

  /**
   * Sets the minimum wait time between action sequence executions.
   *
   * @param minWaitTime the minimum wait time in seconds
   */
  @Override
  public void setMinWaitTime(int minWaitTime) {
    actionConfig.setMinWaitTime(minWaitTime * 1000);
  }

  /**
   * Sets the maximum wait time between action sequence executions.
   *
   * @param maxWaitTime the maximum wait time in seconds
   */
  @Override
  public void setMaxWaitTime(int maxWaitTime) {
    actionConfig.setMaxWaitTime(maxWaitTime * 1000);
  }

  /**
   * Starts the executor in a new thread.
   *
   * @return the thread in which the executor is running
   */
  @Override
  public Thread start() {
    if (running) {
      log.warn("Executor is already running");
      return executorThread;
    }
    running = true;
    executorThread = new Thread(this);
    executorThread.setDaemon(true);
    executorThread.start();
    return executorThread;
  }

  /** Stops the executor. */
  @Override
  public void stop() {
    running = false;
    if (executorThread != null) {
      executorThread.interrupt();
      executorThread = null;
    }
  }

  private void registerApplicationStateChangeListener() {
    applicationContext.registerApplicationStateChangeListener(
        state -> {
          if (state != ApplicationState.RUNNING) {
            if (currentActionSequence == null) {
              return;
            }

            // Use the new instantInterrupt method on the sequence
            currentActionSequence.instantInterrupt();

            log.warn(
                "Application state change detected, interrupted action sequence: {}",
                currentActionSequence.getName());

            currentActionSequence = null;
          }
        });
  }

  private void registerNativeHookListenerForEachKeyBind() {
    log.info("Registering native key and mouse listener");

    GlobalScreen.addNativeMouseListener(
        new NativeMouseListener() {
          @Override
          public void nativeMouseReleased(NativeMouseEvent nativeEvent) {
            processNativeEvent(null, nativeEvent.getButton(), null);
          }
        });

    GlobalScreen.addNativeKeyListener(
        new NativeKeyListener() {
          @Override
          public void nativeKeyReleased(NativeKeyEvent nativeEvent) {
            String keyText = NativeKeyEvent.getKeyText(nativeEvent.getKeyCode());
            /*
             * We have to do this here, since getKeyText is localized
             * and will always return the respective Key in the language
             * the machine is localized in.
             *
             * For example in GER Strg is returned instead of CTRL
             */
            if (nativeEvent.getKeyCode() == NativeKeyEvent.VC_CONTROL) {
              keyText = "CTRL";
            } else if (nativeEvent.getKeyCode() == NativeKeyEvent.VC_ALT) {
              keyText = "ALT";
            } else if (nativeEvent.getKeyCode() == NativeKeyEvent.VC_SHIFT) {
              keyText = "SHIFT";
            }
            processNativeEvent(keyText, null, nativeEvent);
          }
        });
  }

  private void processNativeEvent(
      String keyText, Integer mouseButton, NativeKeyEvent nativeKeyEvent) {
    if (isActionSequenceInactive()) {
      return;
    }

    Action currentAction = getCurrentExecutingAction();
    if (currentAction != null) {
      handleCurrentActionInterruption(keyText, mouseButton, nativeKeyEvent, currentAction);
    }
  }

  private boolean isActionSequenceInactive() {
    return currentActionSequence == null || !currentActionSequence.isActive();
  }

  private Action getCurrentExecutingAction() {
    if (currentActionSequence == null) return null;
    return currentActionSequence.getActions().stream()
        .filter(Action::isExecuting)
        .findFirst()
        .orElse(null);
  }

  private void handleCurrentActionInterruption(
      String keyText, Integer mouseButton, NativeKeyEvent nativeKeyEvent, Action currentAction) {
    String actionKey = currentAction.getActionKey().getKey();
    boolean isKeyBindMatched =
        actionKey.equalsIgnoreCase(keyText)
            || (mouseButton != null && actionKey.equals("MOUSE" + mouseButton));

    if (isKeyBindMatched) {
      hasReleasedAnyKey = true;

      // Interrupt the action
      currentAction.interrupt();

      log.info(
          "Interruption detected with {}: {}",
          nativeKeyEvent != null ? "Key" : "Mousebutton",
          actionKey);
    }
  }

  /** The main execution loop for the executor. */
  @Override
  public void run() {
    while (running && !Thread.currentThread().isInterrupted()) {
      if (!hasReleasedAnyKey && !isWaitTimeExceeded()) {
        if (applicationContext.isCheckForCS2Focus()) {
          handleApplicationState();
        }
        try {
          Thread.sleep(actionConfig.getInterruptCheckInterval());
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          return;
        }
        continue;
      }
      if (isApplicationRunning() && !actionSequenceRepository.getActionSequences().isEmpty()) {
        if (processCurrentActionSequence()) continue;
        chooseAndDispatchRandomSequence();
      }
      updateWaitTime();
    }
  }

  private boolean isWaitTimeExceeded() {
    return Instant.now().toEpochMilli() - lastCycle >= lastWaitTime;
  }

  private boolean isApplicationRunning() {
    return applicationContext.getApplicationState() == ApplicationState.RUNNING;
  }

  private boolean processCurrentActionSequence() {
    if (hasReleasedAnyKey) {
      hasReleasedAnyKey = false;

      // Check if we have a valid sequence to process
      if (currentActionSequence == null || !currentActionSequence.isActive()) {
        return true;
      }

      // Check if the sequence is interrupted
      if (currentActionSequence.isInterrupted()) {
        log.info(
            "Sequence {} is interrupted, skipping further processing",
            currentActionSequence.getName());
        return true;
      }

      // Find any interrupted action
      Action currentAction = findInterruptedAction();

      // If we found an interrupted action with a delay, try to continue it
      if (executeDelayedActionIfNeeded(currentAction)) {
        return true;
      }

      // Check if we need to wait before processing the next sequence
      int remainingWaitTime = calculateRemainingWaitTime();
      if (remainingWaitTime > 0) {
        lastCycle = Instant.now().toEpochMilli();
        return true;
      }
    }
    return false;
  }

  private Action findInterruptedAction() {
    if (currentActionSequence == null) return null;

    return currentActionSequence.getActions().stream()
        .filter(Action::isInterrupted)
        .findFirst()
        .orElse(null);
  }

  private boolean executeDelayedActionIfNeeded(Action currentAction) {
    if (currentAction == null || currentAction.getInterval().isEmpty()) return false;

    // Check if the sequence is interrupted
    if (currentActionSequence != null && currentActionSequence.isInterrupted()) {
      log.info(
          "Sequence {} is interrupted, not redispatching action {}",
          currentActionSequence.getName(),
          currentAction);
      return false;
    }

    Instant now = Instant.now();
    Instant delayedAt = currentAction.getExpectedEnding();
    if (delayedAt != null) {
      long remainingTimeMs = delayedAt.toEpochMilli() - now.toEpochMilli();
      if (remainingTimeMs > 0) {
        log.debug("Continuing action {} for {} ms (redispatched)", currentAction, remainingTimeMs);
        try {
          actionSequenceDispatcher.redispatch(currentAction, remainingTimeMs);
          return true;
        } catch (Exception e) {
          log.error("Error redispatching action {}", currentAction, e);
          if (currentActionSequence != null) {
            currentActionSequence.interrupt();
          }
          return false;
        }
      }
    }
    return false;
  }

  private int calculateRemainingWaitTime() {
    return Math.max(0, lastWaitTime - (int) (Instant.now().toEpochMilli() - lastCycle));
  }

  private synchronized void chooseAndDispatchRandomSequence() {
    // Make sure we don't have any running actions before starting a new sequence
    actionSequenceDispatcher.discardAllRunningActions();

    // Update the cache to get the latest sequences
    actionSequenceRepository.updateActionSequencesCache();

    List<ActionSequence> sequences =
        actionSequenceRepository.getActionSequences().stream()
            .filter(ActionSequence::isActive)
            .toList();

    if (!sequences.isEmpty()) {
      int randomIndex = ThreadLocalRandom.current().nextInt(0, sequences.size());
      ActionSequence selectedSequence = sequences.get(randomIndex);

      // Store reference before dispatching
      currentActionSequence = selectedSequence;

      try {
        // Dispatch the sequence
        actionSequenceDispatcher.dispatchSequence(selectedSequence);

        // Check if the sequence was interrupted during dispatch
        if (selectedSequence.isInterrupted()) {
          log.info("Sequence {} was interrupted during dispatch", selectedSequence.getName());
        } else {
          log.info("Sequence {} was successfully dispatched", selectedSequence.getName());
        }
      } catch (Exception e) {
        log.error("Error dispatching sequence {}", selectedSequence.getName(), e);
      } finally {
        // Clear the reference after dispatch is complete
        currentActionSequence = null;
      }
    } else {
      log.warn("No active ActionSequences found.");
    }
  }

  private void updateWaitTime() {
    int waitTime =
        ThreadLocalRandom.current()
            .nextInt(actionConfig.getMinWaitTime(), actionConfig.getMaxWaitTime());
    lastCycle = Instant.now().toEpochMilli();
    lastWaitTime = waitTime;
  }

  private void handleApplicationState() {
    long currentTime = Instant.now().toEpochMilli();
    if (currentTime - lastFocusCheckTime < actionConfig.getFocusCheckInterval()) return;
    lastFocusCheckTime = currentTime;

    ApplicationState currentState = applicationContext.getApplicationState();

    // Check if focus was gained
    if (currentState == ApplicationState.AWAITING && focusManager.isApplicationWindowInFocus()) {
      applicationContext.setApplicationState(ApplicationState.RUNNING);
      log.info("ApplicationState changed to: RUNNING");
    }
    // Check if focus was lost
    else if (currentState == ApplicationState.RUNNING
        && !focusManager.isApplicationWindowInFocus()) {
      // Interrupt current sequence if it exists
      if (currentActionSequence != null) {
        log.info("Focus lost, interrupting current sequence: {}", currentActionSequence.getName());
        currentActionSequence.instantInterrupt();
      }

      // Update application state
      applicationContext.setApplicationState(ApplicationState.AWAITING);
      log.info("ApplicationState changed to: AWAITING");
    }
  }
}
