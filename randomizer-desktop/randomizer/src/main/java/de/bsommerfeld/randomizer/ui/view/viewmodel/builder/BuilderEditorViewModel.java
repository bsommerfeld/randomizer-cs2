package de.bsommerfeld.randomizer.ui.view.viewmodel.builder;

import com.google.inject.Inject;
import de.bsommerfeld.model.action.Action;
import de.bsommerfeld.model.action.repository.ActionRepository;
import de.bsommerfeld.model.action.repository.ActionSequenceRepository;
import de.bsommerfeld.model.action.sequence.ActionSequence;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import lombok.Getter;

public class BuilderEditorViewModel {

  @Getter
  private final ObjectProperty<ActionSequence> currentActionSequenceProperty =
      new SimpleObjectProperty<>();

  @Getter
  private final ListProperty<Action> currentActionsProperty =
      new SimpleListProperty<>(FXCollections.observableArrayList());

  @Getter private final StringProperty sequenceNameProperty = new SimpleStringProperty();
  @Getter private final StringProperty sequenceDescriptionProperty = new SimpleStringProperty();

  private final ActionRepository actionRepository;
  private final ActionSequenceRepository actionSequenceRepository;

  @Inject
  public BuilderEditorViewModel(
      ActionRepository actionRepository, ActionSequenceRepository actionSequenceRepository) {
    this.actionRepository = actionRepository;
    this.actionSequenceRepository = actionSequenceRepository;
  }

  public void saveActionSequence() {
    deleteOldActionSequenceIfNeeded();
    ActionSequence actionSequence = craftActionSequence();
    actionSequenceRepository.saveActionSequence(actionSequence);
  }

  /** Deletes the old action sequence, when a rename happened. */
  private void deleteOldActionSequenceIfNeeded() {
    ActionSequence actionSequence = currentActionSequenceProperty.get();
    if (actionSequence.getName().equals(sequenceNameProperty.get())) {
      return;
    }
    actionSequenceRepository.deleteActionSequence(actionSequence);
  }

  public void addRandomActions(int count) {
    List<Action> allActions = new ArrayList<>(actionRepository.getActions().keySet());
    int randomCount = ThreadLocalRandom.current().nextInt(1, count);
    for (int i = 0; i < randomCount; i++) {
      int randomIndex = (int) (Math.random() * allActions.size());
      Action randomAction = allActions.get(randomIndex);
      currentActionsProperty.add(randomAction);
    }
  }

  public void setActions(List<Action> actions) {
    currentActionsProperty.setAll(actions);
  }

  private ActionSequence craftActionSequence() {
    ActionSequence actionSequence = new ActionSequence(sequenceNameProperty.get());
    actionSequence.setActions(new ArrayList<>(currentActionsProperty.get()));
    actionSequence.setDescription(sequenceDescriptionProperty.get());
    return actionSequence;
  }

  public List<ActionSequence> getActionSequences() {
    return actionSequenceRepository.getActionSequences();
  }
}
