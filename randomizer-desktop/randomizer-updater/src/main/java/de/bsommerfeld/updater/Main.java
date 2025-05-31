package de.bsommerfeld.updater;

import de.bsommerfeld.updater.util.JarFileUtil;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicProgressBarUI;

public class Main {

  private static final Color BACKGROUND_START = new Color(26, 26, 46); // #1a1a2e
  private static final Color BACKGROUND_END = new Color(22, 33, 62); // #16213e
  private static final Color BORDER_COLOR = new Color(82, 40, 126, 179); // rgba(82, 40, 126, 0.7)
  private static final Color TEXT_COLOR = new Color(224, 225, 249); // #e0e1f9
  private static final Color ACCENT_COLOR = new Color(47, 39, 206); // #2F27CE
  private static final Color SUCCESS_COLOR = new Color(40, 167, 69); // Green
  private static final Color ERROR_COLOR = new Color(220, 53, 69); // Red

  private JFrame mainFrame;
  private JLabel statusLabel;
  private JProgressBar progressBar;
  private JLabel versionComparisonLabel;

  public static void main(String[] args) {
    Thread.currentThread().setUncaughtExceptionHandler(new UpdaterThreadExceptionHandler());
    SwingUtilities.invokeLater(() -> new Main().initialize(args));
  }

  private void initialize(String[] args) {
    setupUI();

    if (args.length != 0) {
      for (String arg : args) {
        if (arg.startsWith("-randomizerLocation=")) {
          File randomizer = extractFilePath(arg);
          if (!randomizer.exists()) {
            showErrorDialog("Randomizer file does not exist!");
            return;
          }
          checkAndUpdate(randomizer);
          break;
        }
      }
    } else {
      showErrorDialog(
          "No randomizer file specified! Please specify the randomizer file location with -randomizerLocation=<randomizerFilePath>");
    }
  }

  private void setupUI() {
    mainFrame = createMainFrame();
    statusLabel = createStatusLabel();
    progressBar = createProgressBar();
    versionComparisonLabel = createVersionComparisonLabel();

    JPanel topPanel = new JPanel(new BorderLayout());
    topPanel.setOpaque(false);

    topPanel.add(versionComparisonLabel, BorderLayout.CENTER);

    JButton closeButton = new JButton("X");
    closeButton.setForeground(Color.WHITE);
    closeButton.setBackground(ERROR_COLOR);
    closeButton.setFocusPainted(false);
    closeButton.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
    closeButton.addActionListener(
        e -> {
          mainFrame.dispose();
          Updater.close();
          System.exit(0);
        });
    topPanel.add(closeButton, BorderLayout.EAST);

    JPanel contentPanel =
        new JPanel(new BorderLayout(10, 10)) {
          @Override
          protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            GradientPaint gradient =
                new GradientPaint(0, 0, BACKGROUND_START, getWidth(), getHeight(), BACKGROUND_END);
            g2d.setPaint(gradient);
            g2d.fillRect(0, 0, getWidth(), getHeight());
          }
        };

    contentPanel.setBorder(
        BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 2), new EmptyBorder(15, 20, 15, 20)));

    JPanel progressPanel = new JPanel(new BorderLayout());
    progressPanel.setOpaque(false);
    progressPanel.add(progressBar, BorderLayout.CENTER);

    contentPanel.add(topPanel, BorderLayout.NORTH);
    contentPanel.add(statusLabel, BorderLayout.CENTER);
    contentPanel.add(progressPanel, BorderLayout.SOUTH);

    mainFrame.setContentPane(contentPanel);
    mainFrame.setVisible(true);
  }

  private JFrame createMainFrame() {
    JFrame frame = new JFrame();
    frame.setSize(550, 250);
    try {
      frame.setTitle(
          "Randomizer Updater - v"
              + Updater.getVersion(JarFileUtil.getJarFile(), Updater.FileType.UPDATER));
    } catch (IOException e) {
      frame.setTitle("Randomizer Updater");
    }
    frame.setLocationRelativeTo(null);
    frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    frame.setUndecorated(true);

    frame.addWindowListener(
        new WindowAdapter() {
          @Override
          public void windowClosing(WindowEvent e) {
            frame.dispose();
            System.exit(0);
          }
        });
    return frame;
  }

  private JLabel createStatusLabel() {
    JLabel label = new JLabel("Checking for updates...", SwingConstants.CENTER);
    label.setFont(new Font("Segoe UI", Font.PLAIN, 16));
    label.setForeground(TEXT_COLOR);
    return label;
  }

  private JProgressBar createProgressBar() {
    JProgressBar progressBar = new JProgressBar(0, 100);
    progressBar.setStringPainted(true);
    progressBar.setForeground(ACCENT_COLOR);
    progressBar.setOpaque(false);
    progressBar.setBorder(BorderFactory.createEmptyBorder());
    progressBar.setUI(
        new BasicProgressBarUI() {
          @Override
          protected void paintDeterminate(Graphics g, JComponent c) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Insets b = progressBar.getInsets();
            int width = progressBar.getWidth() - b.left - b.right;
            int height = progressBar.getHeight() - b.top - b.bottom;

            if (width <= 0 || height <= 0) {
              g2d.dispose();
              return;
            }

            RoundRectangle2D roundedRect = new RoundRectangle2D.Float(0, 0, width, height, 8, 8);
            g2d.setClip(roundedRect);

            g2d.setColor(BACKGROUND_END);
            g2d.fill(roundedRect);

            int amountFull = getAmountFull(b, width, height);
            RoundRectangle2D progressRect =
                new RoundRectangle2D.Float(0, 0, amountFull, height, 8, 8);
            g2d.setColor(progressBar.getForeground());
            g2d.fill(progressRect);

            g2d.dispose();
          }
        });
    return progressBar;
  }

  private JLabel createVersionComparisonLabel() {
    JLabel label = new JLabel("", SwingConstants.CENTER);
    label.setFont(new Font("Segoe UI", Font.BOLD, 14));
    label.setForeground(TEXT_COLOR);
    return label;
  }

  private File extractFilePath(String arg) {
    String path = arg.substring(arg.indexOf("=") + 1);
    return new File(path);
  }

  private void checkAndUpdate(File randomizer) {
    CompletionStage<Boolean> updateAvailable =
        Updater.isUpdateAvailable(
            randomizer, Updater.RANDOMIZER_VERSION_URL, Updater.FileType.RANDOMIZER);
    CompletionStage<String> randomizerVersion =
        Updater.getVersion(randomizer, Updater.FileType.RANDOMIZER);
    CompletionStage<String> latestVersion =
        Updater.getLatestVersion(Updater.RANDOMIZER_VERSION_URL);

    CompletableFuture<Void> allOf =
        CompletableFuture.allOf(
            updateAvailable.toCompletableFuture(),
            randomizerVersion.toCompletableFuture(),
            latestVersion.toCompletableFuture());

    allOf
        .thenRunAsync(
            () -> {
              String currentVersion = randomizerVersion.toCompletableFuture().join();
              String latestVersionString = latestVersion.toCompletableFuture().join();
              boolean isUpdateAvailable = updateAvailable.toCompletableFuture().join();

              SwingUtilities.invokeLater(
                  () -> {
                    versionComparisonLabel.setText(
                        String.format("%s → %s", currentVersion, latestVersionString));
                    versionComparisonLabel.setForeground(
                        isUpdateAvailable ? ERROR_COLOR : SUCCESS_COLOR);

                    if (isUpdateAvailable) {
                      statusLabel.setText("Updating...");
                      updateRandomizer(randomizer);
                    } else {
                      statusLabel.setText("No update available!");
                      progressBar.setValue(100);
                      progressBar.setString("Up to date!");
                    }
                  });
            })
        .exceptionally(
            e -> {
              SwingUtilities.invokeLater(
                  () -> showErrorDialog("Error during update check: " + e.getMessage()));
              return null;
            });
  }

  private void showErrorDialog(String errorMessage) {
    Color errorBackgroundStart =
        new Color(
            BACKGROUND_START.getRed() + 20,
            BACKGROUND_START.getGreen(),
            BACKGROUND_START.getBlue());
    Color errorBackgroundEnd =
        new Color(
            BACKGROUND_END.getRed() + 20, BACKGROUND_END.getGreen(), BACKGROUND_END.getBlue());

    JPanel panel =
        new JPanel() {
          @Override
          protected void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            GradientPaint gradient =
                new GradientPaint(0, 0, errorBackgroundStart, 0, getHeight(), errorBackgroundEnd);
            g2d.setPaint(gradient);
            g2d.fillRect(0, 0, getWidth(), getHeight());
            g2d.dispose();
          }
        };
    panel.setOpaque(false);
    panel.setLayout(new BorderLayout(10, 10));
    panel.setBorder(
        BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 2), new EmptyBorder(20, 30, 20, 30)));

    JLabel label = new JLabel(errorMessage, SwingConstants.CENTER);
    label.setFont(new Font("Segoe UI", Font.PLAIN, 16));
    label.setForeground(ERROR_COLOR);
    panel.add(label, BorderLayout.CENTER);

    JButton okButton = new JButton("OK");
    okButton.setFocusPainted(false);
    JPanel buttonPanel = new JPanel();
    buttonPanel.setOpaque(false);
    buttonPanel.add(okButton);
    panel.add(buttonPanel, BorderLayout.SOUTH);

    final JDialog dialog = new JDialog(mainFrame, "Error", true);
    okButton.addActionListener(
        e -> {
          dialog.dispose();
          System.exit(0);
        });
    dialog.setUndecorated(true);
    dialog.getRootPane().setOpaque(false);
    dialog.getContentPane().setBackground(new Color(0, 0, 0, 0));

    dialog.setContentPane(panel);
    dialog.pack();
    dialog.setLocationRelativeTo(mainFrame);
    dialog.setVisible(true);

    System.exit(0);
  }

  private void updateRandomizer(File randomizer) {
    CompletableFuture.runAsync(() -> performUpdate(randomizer), SwingUtilities::invokeLater)
        .exceptionally(
            e -> {
              SwingUtilities.invokeLater(() -> showErrorDialog("Update failed: " + e.getMessage()));
              return null;
            });
  }

  private void performUpdate(File randomizer) {
    Updater.update(randomizer, Updater.RANDOMIZER_DOWNLOAD_URL, this::updateProgress)
        .thenRunAsync(() -> onUpdateSuccess(randomizer))
        .exceptionallyAsync(
            e -> {
              SwingUtilities.invokeLater(() -> showErrorDialog("Update failed: " + e.getMessage()));
              return null;
            },
            SwingUtilities::invokeLater);
  }

  private void updateProgress(long bytesRead, long totalBytes) {
    double progress = (double) bytesRead / totalBytes * 100;
    SwingUtilities.invokeLater(
        () -> updateProgressBar((int) progress, String.format("%.1f%%", progress)));
  }

  private void updateProgressBar(int value, String text) {
    progressBar.setValue(value);
    progressBar.setString(text);
  }

  private void onUpdateSuccess(File randomizer) {
    SwingUtilities.invokeLater(
        () -> {
          statusLabel.setText("Update successful!");
          updateProgressBar(100, "Completed!");
          launchRandomizer(randomizer);
        });
  }

  private void launchRandomizer(File randomizer) {
    try {
      Runtime.getRuntime().exec("java -jar " + randomizer.getAbsolutePath());
      Thread.sleep(2000);
      System.exit(0);
    } catch (IOException | InterruptedException ex) {
      SwingUtilities.invokeLater(
          () -> showErrorDialog("Failed to launch updated randomizer: " + ex.getMessage()));
    }
  }
}
