package de.bsommerfeld.model.action;

import de.bsommerfeld.model.action.mapper.KeyMapper;
import de.bsommerfeld.model.action.value.Interval;
import de.bsommerfeld.model.config.keybind.KeyBind;
import de.bsommerfeld.model.util.FocusManager;
import java.awt.*;
import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * Represents an abstract action that can be performed by a robot. The action can have a specified
 * interval, and supports interruptions.
 */
@Getter
@Slf4j
@ToString
public abstract class Action implements Cloneable {

  protected static final KeyMapper KEY_MAPPER = new KeyMapper();
  protected static final Robot KNUFFI;
  private static final int INTERVAL = 50;

  static {
    try {
      KNUFFI = new Robot();
    } catch (AWTException e) {
      throw new RuntimeException(e);
    }
  }

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

  /**
   * Executes an action with a certain delay. If no interval is specified, the action is executed
   * immediately. When an interval is specified, a random delay within the interval is chosen.
   */
  public void execute() {
    if (getInterval().isEmpty()) {
      executeWithDelay(0);
    } else {
      if (getInterval().getMin() >= getInterval().getMax()) {
        getInterval().setMax(getInterval().getMin() + 1); // TODO: could be 11
      }
      long delay =
          ThreadLocalRandom.current().nextInt(getInterval().getMin(), getInterval().getMax());
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
    int keyCode = KEY_MAPPER.getKeyCodeForKey(getActionKey().getKey());
    setExecuting(true);
    setInterrupted(false);

    try {
      performActionStart(keyCode);
      performInterruptibleDelay(delay);

      if (!isInterrupted()) {
        performActionEnd(keyCode);
      } else {
        log.info("Action interrupted, skipping action end for: {}", getActionKey().getKey());
      }
    } finally {
      setExecuting(false);
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
  }

  /** Interrupting the keypress and doesn't wait for the current cycle to end. */
  public void instantInterrupt() {
    interrupted = true;
    performActionEnd(KEY_MAPPER.getKeyCodeForKey(getActionKey().getKey()));
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

    if (delayInMillis <= 0) return;

    while (waitedTime < delayInMillis) {
      if (interrupted) {
        log.info("Delay unterbrochen!");
        return;
      }

      if (!FocusManager.isCs2WindowInFocus()) { // TODO: we don't want to have this here
        log.info("Focus lost, interrupting action: {}", getName());
        interrupt();
        return;
      }

      try {
        Thread.sleep(INTERVAL);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return;
      }

      waitedTime += INTERVAL;
    }
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
