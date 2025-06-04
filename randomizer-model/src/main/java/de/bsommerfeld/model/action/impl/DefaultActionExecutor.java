package de.bsommerfeld.model.action.impl;

import com.google.inject.Singleton;
import de.bsommerfeld.model.action.ActionType;
import de.bsommerfeld.model.action.spi.ActionExecutor;
import java.awt.*;
import lombok.extern.slf4j.Slf4j;

/**
 * Default implementation of the ActionExecutor interface. This class uses a Robot to execute
 * keyboard and mouse actions.
 */
@Slf4j
@Singleton
public class DefaultActionExecutor implements ActionExecutor {

  private final Robot robot;

  /**
   * Creates a new DefaultActionExecutor with a Robot instance.
   *
   * @throws RuntimeException if the Robot cannot be created
   */
  public DefaultActionExecutor() {
    try {
      this.robot = new Robot();
    } catch (AWTException e) {
      log.error("Failed to create Robot instance", e);
      throw new RuntimeException("Failed to create Robot instance", e);
    }
  }

  @Override
  public void executeActionStart(int keyCode, ActionType actionType) {
    switch (actionType) {
      case MOUSE -> robot.mousePress(keyCode);
      case MOUSE_WHEEL -> robot.mouseWheel(keyCode);
      case KEYBOARD -> robot.keyPress(keyCode);
      default -> log.debug("No action to execute for key code: {}", keyCode);
    }
  }

  @Override
  public void executeActionEnd(int keyCode, ActionType actionType) {
    switch (actionType) {
      case MOUSE -> robot.mouseRelease(keyCode);
      case KEYBOARD -> robot.keyRelease(keyCode);
      default -> log.debug("No action to end for key code: {}", keyCode);
    }
  }
}
