package de.bsommerfeld.randomizer.ui.randomizer;

import de.bsommerfeld.randomizer.action.Action;
import de.bsommerfeld.randomizer.action.ActionCatalog;
import de.bsommerfeld.randomizer.action.ActionRunner;
import de.bsommerfeld.randomizer.action.BoundKeys;
import de.bsommerfeld.randomizer.action.FireGate;
import de.bsommerfeld.randomizer.action.PlayedAction;
import de.bsommerfeld.randomizer.config.AppPreferences;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * The "Randomizer" tab. Starts and stops the {@link ActionRunner}, edits its settings and keeps a
 * capped log of the actions it played.
 */
public final class RandomizerTabController {

    private static final int MAX_LOG_ENTRIES = 1000;
    private static final int LONGEST_WAIT_SECONDS = 600;

    private static final String MIN_WAIT_KEY = "randomizer.interval.min";
    private static final String MAX_WAIT_KEY = "randomizer.interval.max";
    /** The commands of the switched-off actions, comma separated. New actions are on until someone turns them off. */
    private static final String DISABLED_KEY = "randomizer.actions.disabled";

    @FXML private Button toggleButton;
    @FXML private Label statusLabel;
    @FXML private Spinner<Integer> minSpinner;
    @FXML private Spinner<Integer> maxSpinner;
    @FXML private VBox actionsBox;
    @FXML private ListView<PlayedAction> logList;

    private final ActionRunner runner;
    private final FireGate gate;
    private final AppPreferences preferences;

    private final Map<Action, CheckBox> checkBoxes = new LinkedHashMap<>();
    /**
     * Runs while the randomizer does. The gate can close without any event reaching this tab, for
     * example when GSI stops or CS2 goes silent.
     */
    private final Timeline livePoll = new Timeline(new KeyFrame(Duration.millis(100), tick -> showLiveState()));

    private BooleanSupplier ensureGsiStarted = () -> false;
    private Supplier<Optional<BoundKeys>> boundKeys = Optional::empty;

    public RandomizerTabController(ActionRunner runner, FireGate gate, AppPreferences preferences) {
        this.runner = runner;
        this.gate = gate;
        this.preferences = preferences;
    }

    @FXML
    private void initialize() {
        livePoll.setCycleCount(Animation.INDEFINITE);
        setUpWaitSpinners();
        setUpActionList();
        logList.setCellFactory(list -> new SummaryCell());
        runner.configure(currentSettings());
        runner.onPlayed(played -> Platform.runLater(() -> prependToLog(played)));
        statusLabel.setText("Randomizer stopped.");
    }

    /**
     * Connects this tab to its neighbours. The main controller calls it after the FXML is loaded.
     *
     * @param ensureGsiStarted starts GSI through the GSI tab, false when that failed
     * @param boundKeys        reads the keybind files anew, empty when none could be loaded
     */
    public void init(BooleanSupplier ensureGsiStarted, Supplier<Optional<BoundKeys>> boundKeys) {
        this.ensureGsiStarted = ensureGsiStarted;
        this.boundKeys = boundKeys;
        showBoundKeys(boundKeys.get());
    }

    private static final class SummaryCell extends ListCell<PlayedAction> {
        @Override
        protected void updateItem(PlayedAction item, boolean empty) {
            super.updateItem(item, empty);
            setText(empty || item == null ? null : item.summary());
        }
    }

    // ---- settings ----------------------------------------------------------------------------

    /**
     * The two spinners push each other so min stays below max. Not editable on purpose. Typed text
     * that is no number makes a JavaFX spinner throw on commit, and holding the arrow is quick enough.
     */
    private void setUpWaitSpinners() {
        int min = storedSeconds(MIN_WAIT_KEY, 5, 1, LONGEST_WAIT_SECONDS - 1);
        int max = Math.max(min + 1, storedSeconds(MAX_WAIT_KEY, 30, 2, LONGEST_WAIT_SECONDS));
        minSpinner.setValueFactory(new IntegerSpinnerValueFactory(1, LONGEST_WAIT_SECONDS - 1, min));
        maxSpinner.setValueFactory(new IntegerSpinnerValueFactory(2, LONGEST_WAIT_SECONDS, max));
        minSpinner.valueProperty().addListener((obs, old, value) -> {
            if (value >= maxSpinner.getValue()) {
                maxSpinner.getValueFactory().setValue(value + 1);
            }
            settingChanged(MIN_WAIT_KEY, String.valueOf(value));
        });
        maxSpinner.valueProperty().addListener((obs, old, value) -> {
            if (value <= minSpinner.getValue()) {
                minSpinner.getValueFactory().setValue(value - 1);
            }
            settingChanged(MAX_WAIT_KEY, String.valueOf(value));
        });
    }

    private int storedSeconds(String key, int fallback, int lowest, int highest) {
        return clampedSeconds(preferences.getString(key, ""), fallback, lowest, highest);
    }

    /** The stored text as seconds within the bounds. A hand-edited file may hold anything. */
    static int clampedSeconds(String stored, int fallback, int lowest, int highest) {
        try {
            return Math.clamp(Integer.parseInt(stored), lowest, highest);
        } catch (NumberFormatException notANumber) {
            return fallback;
        }
    }

    private void setUpActionList() {
        List<String> disabled = List.of(preferences.getString(DISABLED_KEY, "").split(",")); // a hand edit may repeat one, Set.of would throw
        for (Action action : ActionCatalog.ALL) {
            CheckBox checkBox = new CheckBox(action.name());
            checkBox.setContentDisplay(ContentDisplay.RIGHT); // the key hint goes behind the name
            checkBox.setSelected(!disabled.contains(action.command()));
            checkBox.selectedProperty().addListener((obs, was, selected) ->
                    settingChanged(DISABLED_KEY, disabledCommands()));
            checkBoxes.put(action, checkBox);
            actionsBox.getChildren().add(checkBox);
        }
    }

    private String disabledCommands() {
        return checkBoxes.entrySet().stream()
                .filter(entry -> !entry.getValue().isSelected())
                .map(entry -> entry.getKey().command())
                .collect(Collectors.joining(","));
    }

    private List<Action> enabledActions() {
        return checkBoxes.entrySet().stream()
                .filter(entry -> entry.getValue().isSelected())
                .map(Map.Entry::getKey)
                .toList();
    }

    private ActionRunner.Settings currentSettings() {
        return new ActionRunner.Settings(minSpinner.getValue(), maxSpinner.getValue(), enabledActions());
    }

    /** A changed setting applies to the running randomizer at once and is remembered for the next start. */
    private void settingChanged(String key, String value) {
        runner.configure(currentSettings());
        try {
            preferences.setString(key, value);
        } catch (IOException e) {
            statusLabel.setText("Setting not saved: " + e.getMessage());
        }
    }

    /** Shows each action's key behind its name and greys out the ones that cannot fire. The mouse move needs no key. */
    private void showBoundKeys(Optional<BoundKeys> keys) {
        checkBoxes.forEach((action, checkBox) -> {
            Optional<BoundKeys> keysToShow = keys.filter(any -> !action.command().equals(Action.MOUSE_MOVE));
            checkBox.setGraphic(keysToShow.map(bound -> keyHint(action, bound)).orElse(null));
            checkBox.setDisable(keysToShow.isPresent() && keysToShow.get().pressableKeyFor(action.command()).isEmpty());
        });
    }

    /** The key as a key cap, or in plain muted text why there is none. */
    private static Label keyHint(Action action, BoundKeys keys) {
        Label hint = new Label(keyHintText(action, keys));
        hint.getStyleClass().add(keys.pressableKeyFor(action.command()).isPresent() ? "key-cap" : "status");
        return hint;
    }

    static String keyHintText(Action action, BoundKeys keys) {
        Optional<String> pressable = keys.pressableKeyFor(action.command());
        if (pressable.isPresent()) {
            return pressable.get();
        }
        List<String> bound = keys.keysFor(action.command());
        return bound.isEmpty() ? "not bound" : "Key not supported: " + String.join(", ", bound);
    }

    // ---- start and stop ----------------------------------------------------------------------

    @FXML
    private void onToggle() {
        if (runner.isRunning()) {
            stop();
        } else {
            start();
        }
    }

    /** Reads the keybinds, makes sure GSI runs and starts the runner, reporting the first thing that goes wrong. */
    private void start() {
        Optional<BoundKeys> keys = boundKeys.get();
        showBoundKeys(keys);
        if (keys.isEmpty()) {
            statusLabel.setText("No keybind config found. Load one in the \"Configs\" tab.");
            return;
        }
        if (!ensureGsiStarted.getAsBoolean()) {
            statusLabel.setText("GSI did not start. The \"Live (GSI)\" tab says why.");
            return;
        }
        runner.start(keys.get());
        toggleButton.setText("Stop randomizer");
        showLiveState();
        livePoll.play();
    }

    private void stop() {
        runner.stop();
        livePoll.stop();
        toggleButton.setText("Start randomizer");
        statusLabel.setText("Randomizer stopped.");
    }

    /** No countdown to the next action on purpose, it would give the surprise away. */
    private void showLiveState() {
        statusLabel.setText(gate.closedBecause().orElse("Running."));
    }

    // ---- log ---------------------------------------------------------------------------------

    private void prependToLog(PlayedAction played) {
        logList.getItems().addFirst(played);
        if (logList.getItems().size() > MAX_LOG_ENTRIES) {
            logList.getItems().removeLast();
        }
    }
}
