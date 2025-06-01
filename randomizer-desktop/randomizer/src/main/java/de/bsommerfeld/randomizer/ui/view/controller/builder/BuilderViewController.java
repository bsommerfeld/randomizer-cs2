package de.bsommerfeld.randomizer.ui.view.controller.builder;

import com.google.inject.Inject;
import de.bsommerfeld.model.action.sequence.ActionSequence;
import de.bsommerfeld.randomizer.config.RandomizerConfig;
import de.bsommerfeld.randomizer.ui.view.View;
import de.bsommerfeld.randomizer.ui.view.ViewProvider;
import de.bsommerfeld.randomizer.ui.view.controller.builder.filler.BuilderFillerViewController;
import de.bsommerfeld.randomizer.ui.view.viewmodel.builder.BuilderViewModel;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import javafx.beans.binding.BooleanBinding;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

@View
public class BuilderViewController {

  private static final String HBOX_STYLE_CLASS = "builder-sequences-hbox";
  private static final String LABEL_STYLE_CLASS = "builder-sequences-title";
  private static final String ACTION_COUNT_STYLE_CLASS = "logbook-history-entry-action-count";
  private static final String HBOX_SELECTED_STYLE_CLASS = "builder-sequences-hbox-selected";

  private static final double BUTTON_HBOX_SPACING = 10;

  private final ViewProvider viewProvider;
  private final RandomizerConfig randomizerConfig;
  private final BuilderViewModel builderViewModel;

  @FXML private HBox rootHBox;
  @FXML private VBox actionSequencesSection;
  @FXML private VBox actionSequencesVBox;
  @FXML private GridPane contentPane;

  @Inject
  public BuilderViewController(
      ViewProvider viewProvider,
      RandomizerConfig randomizerConfig,
      BuilderViewModel builderViewModel) {
    this.viewProvider = viewProvider;
    this.randomizerConfig = randomizerConfig;
    this.builderViewModel = builderViewModel;
  }

  @FXML
  void onAddSequence(ActionEvent event) {
    builderViewModel.createNewActionSequence();
    builderViewModel.getCurrentActionSequenceProperty().set(null);
    showFiller();
    fillActionSequences();
  }

  @FXML
  void onOpenSequenceFolder(ActionEvent event) {
    try {
      builderViewModel.openSequenceFolder();
    } catch (IOException e) {
      throw new IllegalStateException("Konnte Sequencefolder nicht öffnen, Fehler:", e);
    }
  }

  @FXML
  private void initialize() {
    setupBindToNotShowSequencesSection();

    showFiller();
    fillActionSequences();
  }

  /**
   * Configures the binding and visibility settings for the `actionSequencesSection` UI component.
   *
   * <p>This method ensures that the `actionSequencesSection` is added to the beginning of the
   * `rootHBox` if it is not already present, or repositions it if it is not the first element.
   *
   * <p>The visibility and managed properties of the `actionSequencesSection` are bound to a
   * condition based on the presence of a valid configuration path in the `randomizerConfig` object.
   * Specifically: - The `visibleProperty` of the `actionSequencesSection` is bound to a boolean
   * expression that evaluates to true if the configuration path is not null and not empty. - The
   * `managedProperty` of the `actionSequencesSection` is bound to its `visibleProperty`, ensuring
   * that it is only managed in the UI layout when visible.
   */
  private void setupBindToNotShowSequencesSection() {
    if (!rootHBox.getChildren().contains(actionSequencesSection)) {
      rootHBox.getChildren().addFirst(actionSequencesSection);
    } else if (rootHBox.getChildren().getFirst() != actionSequencesSection) {
      rootHBox.getChildren().remove(actionSequencesSection);
      rootHBox.getChildren().addFirst(actionSequencesSection);
    }

    BooleanBinding configPathIsPresent =
        randomizerConfig
            .getConfigPathProperty()
            .isNotNull()
            .and(randomizerConfig.getConfigPathProperty().isNotEmpty());

    actionSequencesSection.visibleProperty().bind(configPathIsPresent);
    actionSequencesSection.managedProperty().bind(actionSequencesSection.visibleProperty());
  }

  private void showFiller() {
    contentPane
        .getChildren()
        .setAll(viewProvider.requestView(BuilderFillerViewController.class).parent());
  }

  public void fillActionSequences() {
    actionSequencesVBox.getChildren().clear();
    List<ActionSequence> actionSequences = builderViewModel.getActionSequences();
    actionSequences.sort(Comparator.comparing(ActionSequence::getName));

    actionSequences.forEach(
        actionSequence -> {
          HBox sequenceHBox = createSequenceHBox(actionSequence);
          actionSequencesVBox.getChildren().add(sequenceHBox);
        });
  }

  private HBox createSequenceHBox(ActionSequence actionSequence) {
    HBox hBox = new HBox();
    hBox.setCursor(Cursor.HAND);
    hBox.getStyleClass().add(HBOX_STYLE_CLASS);

    Label sequenceLabel = createLabel(actionSequence.getName(), LABEL_STYLE_CLASS);
    hBox.setOnMouseClicked(
        _ -> {
          builderViewModel.getCurrentActionSequenceProperty().set(actionSequence);
          actionSequencesVBox
              .getChildren()
              .forEach(hBox1 -> hBox1.getStyleClass().remove(HBOX_SELECTED_STYLE_CLASS));
          hBox.getStyleClass().add(HBOX_SELECTED_STYLE_CLASS);
          contentPane
              .getChildren()
              .setAll(viewProvider.requestView(BuilderEditorViewController.class).parent());
        });
    hBox.getChildren().add(sequenceLabel);

    HBox buttonHBox = createButtonHBox(actionSequence);
    hBox.getChildren().add(buttonHBox);

    return hBox;
  }

  private HBox createButtonHBox(ActionSequence actionSequence) {
    HBox buttonHBox = new HBox();
    buttonHBox.setAlignment(Pos.CENTER_RIGHT);
    HBox.setHgrow(buttonHBox, Priority.ALWAYS);
    buttonHBox.setSpacing(BUTTON_HBOX_SPACING);

    Label actionsCountLabel =
        createLabel(String.valueOf(actionSequence.getActions().size()), ACTION_COUNT_STYLE_CLASS);
    Button deleteButton = createDeleteButton(actionSequence);
    buttonHBox.getChildren().addAll(actionsCountLabel, deleteButton);

    return buttonHBox;
  }

  private Label createLabel(String text, String styleClass) {
    Label label = new Label(text);
    label.getStyleClass().add(styleClass);
    return label;
  }

  private Button createDeleteButton(ActionSequence actionSequence) {
    Button deleteSequenceButton = new Button("");
    deleteSequenceButton.getStyleClass().add("builder-sequences-delete-button");
    deleteSequenceButton.setOnAction(
        event -> {
          if (builderViewModel.getCurrentActionSequenceProperty().get() != null
              && builderViewModel.getCurrentActionSequenceProperty().get().equals(actionSequence)) {
            builderViewModel.getCurrentActionSequenceProperty().set(null);
          }
          builderViewModel.deleteActionSequence(actionSequence);
          showFiller();
          fillActionSequences();
          event.consume();
        });
    return deleteSequenceButton;
  }
}
