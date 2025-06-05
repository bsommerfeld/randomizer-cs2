package de.bsommerfeld.randomizer.ui.view.viewmodel.settings;

import de.bsommerfeld.model.action.Action;
import de.bsommerfeld.model.action.value.Interval;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import lombok.Getter;

@Getter
public class ActionSettingsViewModel {

  private final ObjectProperty<Action> actionInFocusProperty = new SimpleObjectProperty<>();
  private final IntegerProperty minIntervalProperty = new SimpleIntegerProperty();
  private final IntegerProperty maxIntervalProperty = new SimpleIntegerProperty();

  private final ChangeListener<Number> minIntervalUpdateListener = (_, _, _) -> applyInterval();
  private final ChangeListener<Number> maxIntervalUpdateListener = (_, _, _) -> applyInterval();

  public ActionSettingsViewModel() {
    setupActionInFocusListener();
  }

  private void applyInterval() {
    Action currentAction = actionInFocusProperty.get();
    if (currentAction != null) {
      Interval interval = currentAction.getInterval();
      int newMin = minIntervalProperty.get();
      int newMax = maxIntervalProperty.get();

      if (newMin > newMax) {
        newMax = newMin + 1;
      }

      interval.setMin(newMin);
      interval.setMax(newMax);
    }
  }

  private void addIntervalUpdateListeners() {
    minIntervalProperty.addListener(minIntervalUpdateListener);
    maxIntervalProperty.addListener(maxIntervalUpdateListener);
  }

  private void removeIntervalUpdateListeners() {
    minIntervalProperty.removeListener(minIntervalUpdateListener);
    maxIntervalProperty.removeListener(maxIntervalUpdateListener);
  }

  private void setupActionInFocusListener() {
    actionInFocusProperty.addListener(
        (obs, oldAction, newAction) -> {
          removeIntervalUpdateListeners();

          if (newAction != null) {
            minIntervalProperty.set(newAction.getInterval().getMin());
            maxIntervalProperty.set(newAction.getInterval().getMax());

            addIntervalUpdateListeners();
          }
        });
  }
}
