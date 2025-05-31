package de.bsommerfeld.updater;

import java.awt.*;
import javax.swing.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpdaterThreadExceptionHandler implements Thread.UncaughtExceptionHandler {

  private static final Logger logger = LoggerFactory.getLogger(UpdaterThreadExceptionHandler.class);

  @Override
  public void uncaughtException(Thread t, Throwable e) {
    logger.error("Unbehandelte Ausnahme im Thread {}: {}", t.getName(), e.getMessage(), e);

    if (SwingUtilities.isEventDispatchThread()) {
      showErrorDialog(t, e);
    } else {
      SwingUtilities.invokeLater(() -> showErrorDialog(t, e));
    }
  }

  private void showErrorDialog(Thread t, Throwable e) {
    String errorMessage =
        String.format(
            "<html><body><p>Ein unerwarteter Fehler ist im Thread <b>%s</b> aufgetreten:</p>"
                + "<p>%s</p></body></html>",
            t.getName(), e.getMessage());

    JLabel label = new JLabel(errorMessage);
    label.setFont(new Font("Segoe UI", Font.PLAIN, 14));
    label.setForeground(new Color(220, 53, 69));
    label.setBackground(new Color(248, 249, 250));
    label.setOpaque(true);

    JOptionPane.showMessageDialog(null, label, "Kritischer Fehler", JOptionPane.ERROR_MESSAGE);
  }
}
