package de.bsommerfeld.randomizer.ui.view.viewmodel;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
public class NavigationBarViewModel {


  private final ObjectProperty<Class<?>> selectedView = new SimpleObjectProperty<>();

  public void setSelectedView(Class<?> viewClass) {
    this.selectedView.set(viewClass);
  }
}
