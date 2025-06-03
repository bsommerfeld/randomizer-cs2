package de.bsommerfeld.randomizer.ui.view.viewmodel;

import java.awt.*;
import java.io.IOException;
import java.net.URI;

public class HomeViewModel {

  private static final String GITHUB_LINK = "https://github.com/bsommerfeld/randomizer-cs2";
  private static final String DISCORD_LINK = "https://discord.gg/782s5ExhFy";

  public void openGitHub() throws IOException {
    Desktop.getDesktop().browse(URI.create(GITHUB_LINK));
  }

  public void openDiscord() throws IOException {
    Desktop.getDesktop().browse(URI.create(DISCORD_LINK));
  }
}
