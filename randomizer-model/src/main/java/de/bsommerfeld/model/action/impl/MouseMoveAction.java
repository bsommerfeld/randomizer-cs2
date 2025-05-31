package de.bsommerfeld.model.action.impl;

import de.bsommerfeld.model.action.Action;
import de.bsommerfeld.model.action.ActionKey;
import de.bsommerfeld.model.config.keybind.KeyBind;
import java.awt.*;
import java.util.concurrent.ThreadLocalRandom;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MouseMoveAction extends Action {

  private static final int SMOOTH_STEPS = 50;
  private static final int MAX_MOVE_DISTANCE = 5000;

  public MouseMoveAction() {
    super("Mouse move", ActionKey.of(KeyBind.EMPTY_KEY_BIND.getKey()));
  }

  @Override
  protected void performActionStart(int keycode) {
    try {
      Point startPosition = MouseInfo.getPointerInfo().getLocation();
      int startX = startPosition.x;
      int startY = startPosition.y;

      int deltaX = ThreadLocalRandom.current().nextInt(-MAX_MOVE_DISTANCE, MAX_MOVE_DISTANCE + 1);
      int deltaY = ThreadLocalRandom.current().nextInt(-MAX_MOVE_DISTANCE, MAX_MOVE_DISTANCE + 1);

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
    // Keine Aktion erforderlich
  }

  private void smoothMove(int startX, int startY, int endX, int endY) {
    double dx = (endX - startX) / (double) SMOOTH_STEPS;
    double dy = (endY - startY) / (double) SMOOTH_STEPS;

    for (int step = 1; step <= SMOOTH_STEPS; step++) {
      int x = (int) Math.round(startX + dx * step);
      int y = (int) Math.round(startY + dy * step);
      KNUFFI.mouseMove(x, y);
      KNUFFI.delay(10);
    }
    KNUFFI.mouseMove(endX, endY);
  }
}