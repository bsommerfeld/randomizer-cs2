package de.bsommerfeld.model.action.impl;

import de.bsommerfeld.model.action.Action;
import de.bsommerfeld.model.action.ActionKey;
import de.bsommerfeld.model.config.keybind.KeyBind;
import java.awt.*;
import java.util.concurrent.ThreadLocalRandom;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MouseMoveAction extends Action {

  public MouseMoveAction() {
    super("Mouse move", ActionKey.of(KeyBind.EMPTY_KEY_BIND.getKey()));
  }

  @Override
  protected void performActionStart(int keycode) {
    try {
      Point startPosition = MouseInfo.getPointerInfo().getLocation();
      int startX = startPosition.x;
      int startY = startPosition.y;

      int maxDistance = actionConfig != null ? actionConfig.getMaxMouseMoveDistance() : 5000;
      int deltaX = ThreadLocalRandom.current().nextInt(-maxDistance, maxDistance + 1);
      int deltaY = ThreadLocalRandom.current().nextInt(-maxDistance, maxDistance + 1);

      int endX = startX + deltaX;
      int endY = startY + deltaY;

      Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
      endX = Math.max(0, Math.min(endX, screenSize.width - 1));
      endY = Math.max(0, Math.min(endY, screenSize.height - 1));

      log.debug("Moving mouse smoothly from ({}, {}) to ({}, {})", startX, startY, endX, endY);
      smoothMove(startX, startY, endX, endY);

    } catch (Exception e) {
      log.error("Error during smooth mouse move", e);
    }
  }

  @Override
  protected void performActionEnd(int keycode) {
    // No action required
  }

  private void smoothMove(int startX, int startY, int endX, int endY) {
    int steps = actionConfig != null ? actionConfig.getMouseMoveSteps() : 50;
    int delay = actionConfig != null ? actionConfig.getMouseMoveSmoothDelay() : 10;

    double dx = (endX - startX) / (double) steps;
    double dy = (endY - startY) / (double) steps;

    for (int step = 1; step <= steps; step++) {
      int x = (int) Math.round(startX + dx * step);
      int y = (int) Math.round(startY + dy * step);

      try {
        java.awt.Robot robot = new java.awt.Robot();
        robot.mouseMove(x, y);
        robot.delay(delay);
      } catch (Exception e) {
        log.error("Error during mouse move", e);
      }
    }

    try {
      java.awt.Robot robot = new java.awt.Robot();
      robot.mouseMove(endX, endY);
    } catch (Exception e) {
      log.error("Error during final mouse move", e);
    }
  }
}
