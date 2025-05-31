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
import de.bsommerfeld.model.action.repository.ActionSequenceRepository;
import de.bsommerfeld.model.util.FocusManager;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import lombok.extern.slf4j.Slf4j;

/**
 * The ActionSequenceExecutorRunnable class is a specialized implementation of the Runnable
 * interface, responsible for executing sequences of actions based on certain conditions and events.
 */
@Slf4j
public class ActionSequenceExecutorRunnable implements Runnable {

  private static final int FOCUS_CHECK_INTERVAL = 500; // in ms
  private static final String KEY_RELEASE_LOG = "Interruption erkannt mit {}: {}";

  private static volatile int minWaitTime = 30 * 1000;
  private static volatile int maxWaitTime = 120 * 1000;
  private static volatile boolean waitTimeUpdated = false;

  private final ActionSequenceRepository actionSequenceRepository;
  private final ApplicationContext applicationContext;
  private final ActionSequenceDispatcher actionSequenceDispatcher;

  private long lastFocusCheckTime = 0;
  private volatile ActionSequence currentActionSequence;
  private volatile long lastCycle;
  private volatile int lastWaitTime;
  private volatile boolean hasReleasedAnyKey = false;

  @Inject
  public ActionSequenceExecutorRunnable(
      ActionSequenceRepository actionSequenceRepository,
      ApplicationContext applicationContext,
      ActionSequenceDispatcher actionSequenceDispatcher) {
    this.actionSequenceRepository = actionSequenceRepository;
    this.applicationContext = applicationContext;
    this.actionSequenceDispatcher = actionSequenceDispatcher;
    registerNativeHookListenerForEachKeyBind();
    registerApplicationStateChangeListener();
  }

  /**
   * Sets the minimum wait time for the ActionSequenceExecutorRunnable.
   *
   * @param minWaitTime the minimum wait time to be set, in seconds
   */
  public static void setMinWaitTime(int minWaitTime) {
    ActionSequenceExecutorRunnable.minWaitTime = minWaitTime * 1000;
    waitTimeUpdated = true;
  }

  /**
   * Sets the maximum wait time for the action sequence executor.
   *
   * @param maxWaitTime the maximum wait time in seconds
   */
  public static void setMaxWaitTime(int maxWaitTime) {
    ActionSequenceExecutorRunnable.maxWaitTime = maxWaitTime * 1000;
    waitTimeUpdated = true;
  }

  private void registerApplicationStateChangeListener() {
    applicationContext.registerApplicationStateChangeListener(
        state -> {
          if (state != ApplicationState.RUNNING) {
            Action currentAction = getCurrentExecutingAction();
            if (currentAction != null) {
              currentAction.interrupt(); // TODO: this reactivates the action
              log.info("Interrupted action due to ApplicationState change");
            }
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
      currentAction.interrupt();
      log.info(KEY_RELEASE_LOG, nativeKeyEvent != null ? "Key" : "Mousebutton", actionKey);
    }
  }

  /**
   * The {@code run} method contains the main loop for a thread, which will continue to run until
   * the thread is interrupted. This method performs a series of checks and actions within the loop:
   *
   * <ul>
   *   <li>It checks if no keys have been released, the wait time has not been updated, and if the
   *       wait time has not been exceeded. If all conditions are met, the thread will sleep for 50
   *       milliseconds before rechecking.
   *   <li>It verifies if the application window (Cs2) is in focus, and if it is, it handles the
   *       application state updates and processes or dispatches action sequences based on the
   *       application's running state.
   *   <li>The wait time is reset and updated as needed during each iteration of the loop.
   * </ul>
   *
   * <p>If the thread's sleep is interrupted, a {@code RuntimeException} is thrown.
   */
  @Override
  public void run() {
    while (!Thread.currentThread().isInterrupted()) {
      if (!hasReleasedAnyKey && !waitTimeUpdated && !isWaitTimeExceeded()) {
        handleApplicationState();
        try {
          Thread.sleep(50);
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
        continue;
      }
      if (isApplicationRunning() && !actionSequenceRepository.getActionSequences().isEmpty()) {
        if (processCurrentActionSequence()) continue;
        chooseAndDispatchRandomSequence();
      }
      resetWaitTimeIfUpdated();
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
      if (currentActionSequence == null || !currentActionSequence.isActive()) return true;
      Action currentAction = findInterruptedAction();
      if (currentAction != null && executeDelayedActionIfNeeded(currentAction)) return true;
      int remainingWaitTime = calculateRemainingWaitTime();
      if (remainingWaitTime > 0) {
        lastCycle = Instant.now().toEpochMilli();
        return true;
      }
    }
    return false;
  }

  private Action findInterruptedAction() {
    return currentActionSequence.getActions().stream()
        .filter(Action::isInterrupted)
        .findFirst()
        .orElse(null);
  }

  private boolean executeDelayedActionIfNeeded(Action currentAction) {
    if (currentAction.getInterval().isEmpty()) return false;
    Instant now = Instant.now();
    Instant delayedAt = currentAction.getExpectedEnding();
    if (delayedAt != null) {
      long remainingTimeMs = delayedAt.toEpochMilli() - now.toEpochMilli();
      if (remainingTimeMs > 0) {
        log.debug("Führe Aktion für {} ms fort (redispatched)", remainingTimeMs);
        actionSequenceDispatcher.redispatch(currentAction, remainingTimeMs);
        return true;
      }
    }
    return false;
  }

  private int calculateRemainingWaitTime() {
    return Math.max(0, lastWaitTime - (int) (Instant.now().toEpochMilli() - lastCycle));
  }

  private synchronized void chooseAndDispatchRandomSequence() {
    actionSequenceRepository.updateActionSequencesCache();

    List<ActionSequence> sequences =
        actionSequenceRepository.getActionSequences().stream()
            .filter(ActionSequence::isActive)
            .toList();

    if (!sequences.isEmpty()) {
      int randomIndex = ThreadLocalRandom.current().nextInt(0, sequences.size());
      currentActionSequence = sequences.get(randomIndex);
      actionSequenceDispatcher.dispatchSequence(currentActionSequence);
      log.info("Sequence {} wurde dispatched.", currentActionSequence.getName());
    } else {
      log.warn("Keine aktiven ActionSequences gefunden.");
    }
  }

  private void resetWaitTimeIfUpdated() {
    if (waitTimeUpdated) {
      waitTimeUpdated = false;
    }
  }

  private void updateWaitTime() {
    int waitTime = ThreadLocalRandom.current().nextInt(minWaitTime, maxWaitTime);
    lastCycle = Instant.now().toEpochMilli();
    lastWaitTime = waitTime;
  }

  private void handleApplicationState() {
    long currentTime = Instant.now().toEpochMilli();
    if (currentTime - lastFocusCheckTime < FOCUS_CHECK_INTERVAL) return;
    lastFocusCheckTime = currentTime;

    ApplicationState currentState = applicationContext.getApplicationState();
    if (currentState == ApplicationState.AWAITING && FocusManager.isCs2WindowInFocus()) {
      applicationContext.setApplicationState(ApplicationState.RUNNING);
      log.info("ApplicationState geändert zu: RUNNING");
    } else if (currentState == ApplicationState.RUNNING && !FocusManager.isCs2WindowInFocus()) {
      applicationContext.setApplicationState(ApplicationState.AWAITING);
      log.info("ApplicationState geändert zu: AWAITING");
    }
  }
}
