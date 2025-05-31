package de.bsommerfeld.randomizer.ui.view.viewmodel;

import com.google.inject.Inject;
import de.bsommerfeld.randomizer.bootstrap.RandomizerUpdater;
import java.util.concurrent.CompletionStage;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
public class NavigationBarViewModel {

  private final RandomizerUpdater randomizerUpdater;

  private final ObjectProperty<Class<?>> selectedView = new SimpleObjectProperty<>();

  @Inject
  public NavigationBarViewModel(RandomizerUpdater randomizerUpdater) {
    this.randomizerUpdater = randomizerUpdater;
  }

  public void setSelectedView(Class<?> viewClass) {
    this.selectedView.set(viewClass);
  }

  public CompletionStage<Boolean> isUpdateAvailable() {
    return randomizerUpdater.isRandomizerUpdateAvailable();
  }

  public void runUpdater() {
    randomizerUpdater.runUpdaterIfNeeded();
  }
}
