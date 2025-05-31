package de.bsommerfeld.randomizer.ui.view.controller.settings;

import com.google.inject.Inject;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import de.bsommerfeld.randomizer.bootstrap.RandomizerUpdater;
import de.bsommerfeld.randomizer.ui.view.View;
import de.bsommerfeld.randomizer.ui.view.ViewProvider;
import de.bsommerfeld.randomizer.ui.view.controller.NavigationBarController;
import de.bsommerfeld.randomizer.ui.view.viewmodel.settings.UpdateSettingsViewModel;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.MouseEvent;

@View
public class UpdaterSettingsViewController {

    private final UpdateSettingsViewModel updateSettingsViewModel;
    private final RandomizerUpdater randomizerUpdater; // you don't belong here
    private final ViewProvider viewProvider;

    @FXML private ToggleButton autoUpdateToggleButton;
    @FXML private ToggleButton updateNotifierToggleButton;
    @FXML private Label updateLabel;

    @Inject
    public UpdaterSettingsViewController(
            UpdateSettingsViewModel updateSettingsViewModel,
            ViewProvider viewProvider,
            RandomizerUpdater randomizerUpdater) {
        this.updateSettingsViewModel = updateSettingsViewModel;
        this.randomizerUpdater = randomizerUpdater;
        this.viewProvider = viewProvider;
    }

    @FXML
    private void initialize() {
        bind();
        updateSettingsViewModel.setupProperties();
    }

    @FXML
    public void onUpdateCheck(ActionEvent event) {
        CompletionStage<Boolean> randomizerUpdateAvailable =
                randomizerUpdater.isRandomizerUpdateAvailable();
        CompletionStage<String> randomizerVersion = randomizerUpdater.getRandomizerVersion();

        CompletableFuture<Void> allOf =
                CompletableFuture.allOf(
                        randomizerUpdateAvailable.toCompletableFuture(),
                        randomizerVersion.toCompletableFuture());

        allOf.thenRunAsync(
                () -> {
                    boolean updateAvailable = randomizerUpdateAvailable.toCompletableFuture().join();
                    String version = randomizerVersion.toCompletableFuture().join();

                    if (updateAvailable) {
                        updateLabel.setText("Update Available - v" + version);
                        viewProvider
                                .requestView(NavigationBarController.class)
                                .controller()
                                .triggerUpdateCheck();
                    } else {
                        updateLabel.setText("Randomizer is up to date!");
                    }
                },
                Platform::runLater);
    }

    private void bind() {
        autoUpdateToggleButton
                .selectedProperty()
                .bindBidirectional(updateSettingsViewModel.autoUpdateProperty());
        updateNotifierToggleButton
                .selectedProperty()
                .bindBidirectional(updateSettingsViewModel.updateNotifierProperty());
    }

    public void onRepoLinkClicked(MouseEvent mouseEvent) throws IOException {
        updateSettingsViewModel.browseRepository();
    }
}