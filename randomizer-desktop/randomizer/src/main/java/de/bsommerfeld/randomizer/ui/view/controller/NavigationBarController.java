package de.bsommerfeld.randomizer.ui.view.controller;

import com.google.inject.Inject;
import de.bsommerfeld.randomizer.ui.view.View;
import de.bsommerfeld.randomizer.ui.view.ViewProvider;
import de.bsommerfeld.randomizer.ui.view.controller.builder.BuilderViewController;
import de.bsommerfeld.randomizer.ui.view.viewmodel.NavigationBarViewModel;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import lombok.extern.slf4j.Slf4j;

@View
@Slf4j
public class NavigationBarController {

  private final NavigationBarViewModel navigationBarViewModel;
  private final ViewProvider viewProvider;
  private final Map<Class<?>, Consumer<?>> viewInitializers = new HashMap<>();
  @FXML private ToggleButton homeButton;
  @FXML private ToggleButton randomizerButton;
  @FXML private ToggleButton builderButton;
  @FXML private ToggleButton settingsButton;
  private List<ToggleButton> navButtons;

  @Inject
  public NavigationBarController(
      NavigationBarViewModel navigationBarViewModel, ViewProvider viewProvider) {
    this.navigationBarViewModel = navigationBarViewModel;
    this.viewProvider = viewProvider;
  }

  @FXML
  private void initialize() {
    navButtons = Arrays.asList(homeButton, randomizerButton, builderButton, settingsButton);

    setupViewInitializers();
    setupNavigationButton(homeButton, "Home", HomeViewController.class);
    setupNavigationButton(randomizerButton, "Randomizer", RandomizerViewController.class);
    setupNavigationButton(builderButton, "Builder", BuilderViewController.class);
    setupNavigationButton(settingsButton, "Settings", SettingsViewController.class);

    navigationBarViewModel
        .getSelectedView()
        .addListener((obs, oldView, newView) -> triggerViewChange(newView));

    if (navigationBarViewModel.getSelectedView().get() == null) {
      Platform.runLater(() -> selectButton(homeButton, HomeViewController.class));
    } else {
      Class<?> currentView = navigationBarViewModel.getSelectedView().get();
      navButtons.stream()
          .filter(btn -> getViewClassForButton(btn).equals(currentView))
          .findFirst()
          .ifPresent(btn -> Platform.runLater(() -> btn.setSelected(true)));
    }
  }

  private void setupViewInitializers() {
    viewInitializers.put(
        HomeViewController.class, (Consumer<HomeViewController>) HomeViewController::updateView);
  }

  private void setupNavigationButton(ToggleButton button, String tooltipText, Class<?> viewClass) {
    button.setUserData(viewClass);
    Tooltip tooltip = new Tooltip(tooltipText);
    tooltip.getStyleClass().add("tooltip-user-options");
    Tooltip.install(button, tooltip);

    button
        .selectedProperty()
        .addListener(
            (obs, wasSelected, isSelected) -> {
              if (isSelected) {
                selectButton(button, viewClass);
              } else {
                ensureAtLeastOneButtonSelected(button);
              }
            });
  }

  private void selectButton(ToggleButton selectedButton, Class<?> viewClass) {
    if (navigationBarViewModel.getSelectedView().get() != viewClass) {
      navigationBarViewModel.setSelectedView(viewClass);
    }
    for (ToggleButton btn : navButtons) {
      if (btn != selectedButton && btn.isSelected()) {
        btn.setSelected(false);
      }
    }
    if (!selectedButton.isSelected()) {
      selectedButton.setSelected(true);
    }
  }

  private void ensureAtLeastOneButtonSelected(ToggleButton deselectedButton) {
    boolean anotherButtonIsSelected =
        navButtons.stream().anyMatch(btn -> btn != deselectedButton && btn.isSelected());
    if (!anotherButtonIsSelected) {
      Platform.runLater(() -> deselectedButton.setSelected(true));
    }
  }

  @SuppressWarnings("unchecked")
  private <T> void triggerViewChange(Class<T> newViewClass) {
    if (newViewClass == null) {
      return;
    }

    Consumer<T> initializer = (Consumer<T>) viewInitializers.get(newViewClass);

    if (initializer != null) {
      viewProvider.triggerViewChange(newViewClass, initializer);
    } else {
      viewProvider.triggerViewChange(newViewClass);
    }
  }

  private Class<?> getViewClassForButton(ToggleButton button) {
    return (Class<?>) button.getUserData();
  }
}
