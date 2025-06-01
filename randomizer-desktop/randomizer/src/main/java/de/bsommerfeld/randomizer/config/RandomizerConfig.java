package de.bsommerfeld.randomizer.config;

import de.bsommerfeld.jshepherd.annotation.Comment;
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

  @Key("min.interval")
  private int minInterval = 15;

  @Key("max.interval")
  private int maxInterval = 70;

  @Key("autoupdate.enabled")
  private boolean autoupdateEnabled = false;

  @Key("update.notifier")
  private boolean updateNotifier = true;

  @Key("show.intro")
  private boolean showIntro = true;

  @Key("config.path")
  private String configPath = "";

  @Key("builder.filters.activated")
  private List<String> builderFiltersActivated = new ArrayList<>(KeyBindType.values().length);

  @Key("time.tracked")
  @Comment({"Basically this is just to see, how many hours you've spent with the Randomizer on.", "Please do not change this specific value, it would just be self-sabotage"})
  private long timeTracked = 0L;

  public void setConfigPath(String configPath) {
    this.configPath = configPath;
    configPathProperty.set(configPath);
  }
}
