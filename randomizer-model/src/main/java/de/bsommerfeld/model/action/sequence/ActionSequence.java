package de.bsommerfeld.model.action.sequence;

import de.bsommerfeld.model.action.Action;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * The ActionSequence class represents a sequence of actions to be executed. It includes
 * functionalities to manage the actions, track the sequence's state, and provide additional context
 * about the sequence.
 */
@Getter
@Setter
@Slf4j
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ActionSequence {

  /**
   * The name of the ActionSequence instance. This field is included in the equals and hashCode
   * methods for comparison.
   */
  @EqualsAndHashCode.Include private final String name;

  /**
   * A list that holds the actions to be executed within the action sequence. This list is final and
   * cannot be modified directly, ensuring the integrity of the action sequence. Actions should be
   * set or modified using the relevant methods provided in the {@link ActionSequence} class.
   */
  private final List<Action> actions = new ArrayList<>();

  /**
   * Indicates whether the ActionSequence is currently active.
   *
   * <p>If true, the sequence of actions defined in the ActionSequence can be executed; otherwise,
   * it will not be executed.
   */
  boolean active = true;

  /**
   * Indicates whether the ActionSequence has been interrupted.
   *
   * <p>If true, the sequence execution should be stopped as soon as possible. -- GETTER -- Checks
   * if this ActionSequence is interrupted.
   *
   * @return true if the sequence is interrupted, false otherwise
   */
  private volatile boolean interrupted = false;

  /**
   * Provides a textual description of the ActionSequence. This is intended to offer additional
   * context or details about the sequence of actions.
   */
  private String description = "No description provided";

  public ActionSequence(String name) {
    this.name = name;
  }

  /**
   * Sets the actions for the ActionSequence. Clears any existing actions and adds all the actions
   * from the provided list.
   *
   * @param actions the list of actions to set
   */
  public void setActions(List<Action> actions) {
    this.actions.clear();
    this.actions.addAll(actions);
  }

  /**
   * Interrupts this ActionSequence and all its actions. This will stop the execution of the
   * sequence as soon as possible.
   */
  public void interrupt() {
    this.interrupted = true;
    actions.forEach(Action::interrupt);
    log.info("ActionSequence '{}' has been interrupted", name);
  }

  /**
   * Interrupts this ActionSequence and all its actions immediately. This will immediately stop all
   * actions in the sequence.
   */
  public void instantInterrupt() {
    this.interrupted = true;
    actions.forEach(Action::instantInterrupt);
    log.info("ActionSequence '{}' has been immediately interrupted", name);
  }

  /** Resets the interrupted state of this ActionSequence and all its actions. */
  public void resetInterrupted() {
    this.interrupted = false;
    actions.forEach(Action::normalize);
    log.info("ActionSequence '{}' interrupted state has been reset", name);
  }

  @Override
  public String toString() {
    String eventsString = actions.stream().map(Action::toString).collect(Collectors.joining(", "));
    return "ActionSequence{name='" + name + "', actions=[" + eventsString + "]}";
  }
}
