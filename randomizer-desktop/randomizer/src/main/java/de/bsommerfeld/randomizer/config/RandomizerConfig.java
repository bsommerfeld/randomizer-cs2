package de.bsommerfeld.randomizer.config;

import de.bsommerfeld.jshepherd.annotation.Comment;
import de.bsommerfeld.jshepherd.annotation.CommentSection;
import de.bsommerfeld.jshepherd.annotation.Key;
import de.bsommerfeld.jshepherd.core.ConfigurablePojo;
import de.bsommerfeld.model.config.keybind.KeyBindType;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class RandomizerConfig extends ConfigurablePojo<RandomizerConfig> {

  private final StringProperty configPathProperty = new SimpleStringProperty();

  @CommentSection({"Randomizer Specific Configuration", "------"})
  @Comment({"The Min Interval, for determining a random number in a span."})
  @Key("min.interval")
  private int minInterval = 15;

  @Comment({"The Max Interval, for determining a random number in a span."})
  @Key("max.interval")
  private int maxInterval = 70;

  @CommentSection({"General Settings", "------"})
  @Comment({"Whether or not the intro screen should be shown."})
  @Key("show.intro")
  private boolean showIntro = true;

  @CommentSection({"Functional Settings", "------"})
  @Comment({"The config path of the CS2 configuration."})
  @Key("config.path")
  private String configPath = "";

  @CommentSection({"Builder Settings", "------"})
  @Comment({
    "The list of filters that should be activated for the builder.",
    "Personal Preference that is stored."
  })
  @Key("builder.filters.activated")
  private List<String> builderFiltersActivated = new ArrayList<>(KeyBindType.values().length);

  public void setConfigPath(String configPath) {
    this.configPath = configPath;
    configPathProperty.set(configPath);
  }
}
