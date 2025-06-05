package de.bsommerfeld.model.action;

import com.google.inject.Inject;
import de.bsommerfeld.model.ApplicationContext;
import de.bsommerfeld.model.action.config.ActionConfig;
import de.bsommerfeld.model.action.mapper.KeyMapper;
import de.bsommerfeld.model.action.spi.ActionExecutor;
import de.bsommerfeld.model.action.spi.FocusManager;
import de.bsommerfeld.model.action.value.Interval;
import de.bsommerfeld.model.config.keybind.KeyBind;
import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * Represents an abstract action that can be performed. The action can have a specified interval,
 * and supports interruptions.
 */
@Getter
@Slf4j
@ToString
public abstract class Action implements Cloneable {

  protected static final KeyMapper KEY_MAPPER = new KeyMapper();

  protected static ApplicationContext applicationContext;
  protected static ActionExecutor actionExecutor;
  protected static FocusManager focusManager;
  protected static ActionConfig actionConfig;
  private final transient ActionKey actionKey;
  private final transient ActionType actionType;

  /** The name representing a unique identifier. */
  private final String name;

  /**
   * Represents an interval with a start and end time.
   *
   * <p>Default values are 0
   */
  @Setter private Interval interval = Interval.of(0, 1);

  @Setter(AccessLevel.PROTECTED)
  @ToString.Exclude
  private transient volatile boolean interrupted = false;

  @Setter(AccessLevel.PROTECTED)
  @ToString.Exclude
  private transient volatile boolean executing = false;

  @Setter(AccessLevel.PROTECTED)
  @ToString.Exclude
  private transient volatile Instant expectedEnding = null;

  private transient volatile long delay = -1L;

  public Action(String name, ActionKey actionKey) {
    this.name = name;
    this.actionKey = actionKey;
    this.actionType =
        isMouseEvent()
            ? ActionType.MOUSE
            : isMouseWheelEvent()
                ? ActionType.MOUSE_WHEEL
                : hasKey() ? ActionType.KEYBOARD : ActionType.CUSTOM;
  }

  @Inject
  public static void setDependencies(
      ApplicationContext context,
      ActionExecutor executor,
      FocusManager manager,
      ActionConfig config) {
    applicationContext = context;
    actionExecutor = executor;
    focusManager = manager;
    actionConfig = config;
  }

  /**
   * Executes an action with a certain delay. If no interval is specified, the action is executed
   * immediately. When an interval is specified, a random delay within the interval is chosen.
   */
  public void execute() {
    if (getInterval().isEmpty()) {
      executeWithDelay(0);
    } else {
      if (getInterval().getMin() >= getInterval().getMax()) {
        // Add a small buffer to ensure max is greater than min
        getInterval().setMax(getInterval().getMin() + 1);
      }
      delay = ThreadLocalRandom.current().nextInt(getInterval().getMin(), getInterval().getMax());
      executeWithDelay(delay);
    }
  }

  /**
   * Determines if the action has ended based on the expected ending time.
   *
   * @return {@code true} if the expected ending time is set and is before the current time,
   *     otherwise {@code false}.
   */
  public boolean hasEnded() {
    return expectedEnding != null && expectedEnding.isBefore(Instant.now());
  }

  /**
   * Executes an action with a specified delay. It begins by performing an action start using a key
   * code, waits for the specified delay allowing for interruption, and then, if not interrupted,
   * performs the action end.
   *
   * @param delay the time period (in milliseconds) to wait between performing the start and end of
   *     the action
   */
  public void executeWithDelay(long delay) {
    log.debug("DEBUGGING: Starting executeWithDelay for {} with delay {}", getName(), delay);
    int keyCode = KEY_MAPPER.getKeyCodeForKey(getActionKey().getKey());
    setExecuting(true);
    setInterrupted(false);

    try {
      performActionStart(keyCode);
      log.debug("DEBUGGING: About to start delay for {}", getName());
      performInterruptibleDelay(delay);
      log.debug("DEBUGGING: Delay finished for {}, interrupted: {}", getName(), isInterrupted());

      if (!isInterrupted()) {
        performActionEnd(keyCode);
        log.debug("DEBUGGING: Action end performed for {}", getName());
      } else {
        log.info("Action interrupted, skipping action end for: {}", getActionKey().getKey());
      }
    } finally {
      setExecuting(false);
      log.debug("DEBUGGING: Set executing=false for {}", getName());
    }
  }

  /**
   * Interrupts the current process.
   *
   * <p>This method sets the interrupted flag to true, indicating that the process should be
   * terminated as soon as possible.
   */
  public void interrupt() {
    interrupted = true;
    executing = false;
  }

  /**
   * Resets the state of the action to its initial state.
   *
   * <p>This method sets the flags `interrupted` and `executing` to `false`, effectively normalizing
   * the state of the action. It is used to revert the action to a non-interrupted and non-executing
   * state.
   */
  public void normalize() {
    interrupted = false;
    executing = false;
  }

  /** Interrupting the keypress and doesn't wait for the current cycle to end. */
  public void instantInterrupt() {
    interrupt();
    int keyCode = KEY_MAPPER.getKeyCodeForKey(getActionKey().getKey());
    performActionEnd(keyCode);
  }

  /**
   * Performs a delay that can be interrupted. If the interval is not empty, it calculates the
   * expected end time and calls the interruptible delay method.
   *
   * @param delay the delay duration in milliseconds
   */
  protected void performInterruptibleDelay(long delay) {
    if (!getInterval().isEmpty()) {
      expectedEnding = Instant.now().plusMillis(delay);
      interruptibleDelay(delay);
    }
  }

  private void interruptibleDelay(long delayInMillis) {
    int waitedTime = 0;
    int checkInterval = actionConfig != null ? actionConfig.getInterruptCheckInterval() : 50;

    if (delayInMillis <= 0) return;

    long start = System.currentTimeMillis();

    while (waitedTime < delayInMillis) {
      if (interrupted) {
        log.info("Delay interrupted!");
        return;
      }

      if (applicationContext.isCheckForCS2Focus()
          && focusManager != null
          && !focusManager.isApplicationWindowInFocus()) {
        log.info("Focus lost, interrupting action: {}", getName());
        interrupt();
        return;
      }

      /*
       * Due to this, we have a slight off-time of a few milliseconds.
       * Makes 2385 ms to 2572 ms and 6664 ms to 7166 ms
       */
      try {
        Thread.sleep(checkInterval);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return;
      }

      waitedTime += checkInterval;
    }

    log.info("{} ran for {} ms", this.getName(), System.currentTimeMillis() - start);
  }

  private boolean isMouseWheelEvent() {
    return actionKey.getKey().toUpperCase().startsWith("MWHEEL");
  }

  private boolean isMouseEvent() {
    return actionKey.getKey().toUpperCase().startsWith("MOUSE");
  }

  private boolean hasKey() {
    return !actionKey.getKey().equals(KeyBind.EMPTY_KEY_BIND.getKey());
  }

  @Override
  public Action clone() throws CloneNotSupportedException {
    try {
      Action cloned = (Action) super.clone();
      cloned.setInterval(Interval.of(this.interval.getMin(), this.interval.getMax()));
      return cloned;
    } catch (CloneNotSupportedException e) {
      throw new AssertionError();
    }
  }

  /**
   * Triggers the start of a specific action based on the provided key code.
   *
   * @param keyCode the code of the key that was pressed to initiate the action
   */
  protected abstract void performActionStart(int keyCode);

  /**
   * Executes the final action corresponding to the specified key code.
   *
   * @param keyCode the key code representing a specific action to end
   */
  protected abstract void performActionEnd(int keyCode);
}
