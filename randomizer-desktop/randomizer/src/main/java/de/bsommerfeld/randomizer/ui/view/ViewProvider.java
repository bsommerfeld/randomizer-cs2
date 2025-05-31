package de.bsommerfeld.randomizer.ui.view;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class ViewProvider {

  private final Map<Class<?>, ViewWrapper<?>> viewMap = new ConcurrentHashMap<>();
  private final Map<Class<?>, Consumer<?>> listenerMap = new ConcurrentHashMap<>();
  private final ViewLoader viewLoader = new ViewLoader();

  /**
   * Registers a listener that responds to changes in the specified view class.
   *
   * @param <T> the type of the view class
   * @param viewClass the class of the view to listen for changes on
   * @param listener the consumer that performs actions when the view changes
   */
  public <T> void registerViewChangeListener(Class<T> viewClass, Consumer<T> listener) {
    listenerMap.put(viewClass, listener);
  }

  /**
   * Requests a view of the specified class, loading it if necessary.
   *
   * @param viewClass the class of the view to request
   * @return a {@link ViewWrapper} containing the requested view and its controller
   * @throws IllegalStateException if the view could not be instantiated
   */
  @SuppressWarnings("unchecked")
  public <T> ViewWrapper<T> requestView(Class<T> viewClass) {
    checkForViewAnnotation(viewClass);
    // Versuche zunächst, die View aus der Map zu holen
    ViewWrapper<T> viewWrapper = (ViewWrapper<T>) viewMap.get(viewClass);
    if (viewWrapper == null) {
      // Lade die View in einer separaten Methode, ohne seitliche Effekte in computeIfAbsent
      viewWrapper = loadViewSafely(viewClass);
      // Füge sie nur ein, wenn noch nicht vorhanden
      ViewWrapper<T> previousValue = (ViewWrapper<T>) viewMap.putIfAbsent(viewClass, viewWrapper);
      if (previousValue != null) {
        viewWrapper = previousValue;
      }
    }
    return viewWrapper;
  }

  private <T> ViewWrapper<T> loadViewSafely(Class<T> viewClass) {
    try {
      return viewLoader.loadView(viewClass);
    } catch (Exception e) {
      throw new IllegalStateException("Could not instantiate view: " + viewClass.getName(), e);
    }
  }

  /**
   * Triggers a view change by notifying the registered listener for the specified view class.
   *
   * <p>If the view is not already loaded, it will be loaded using {@link #requestView(Class)}.
   * After the view is loaded, the registered listener (if any) will be executed.
   *
   * @param viewClass the class of the view whose change listener should be triggered
   * @param <T> the type of the view class
   */
  public <T> void triggerViewChange(Class<T> viewClass) {
    triggerViewChange(viewClass, null);
  }

  /**
   * Triggers a view change by notifying the registered listener for the specified view class and
   * optionally configuring the view controller.
   *
   * <p>If the view is not already loaded, it will be loaded using {@link #requestView(Class)}.
   * After the view is loaded, the registered listener (if any) will be executed, and the provided
   * configurator will be applied to the view's controller.
   *
   * @param viewClass the class of the view whose change listener should be triggered
   * @param viewConfigurator an optional {@link Consumer} to configure the view controller before or
   *     after the listener is triggered (can be {@code null})
   * @param <T> the type of the view class
   */
  @SuppressWarnings("unchecked")
  public <T> void triggerViewChange(Class<T> viewClass, Consumer<T> viewConfigurator) {
    // Check if the view is already loaded
    ViewWrapper<T> viewWrapper = (ViewWrapper<T>) viewMap.get(viewClass);
    if (viewWrapper == null) {
      // Load the view if it is not already loaded
      viewWrapper = requestView(viewClass);
    }

    // Get the controller from the ViewWrapper
    T controller = viewWrapper.controller();

    // Apply the registered listener, if any is registered
    Consumer<T> listener = (Consumer<T>) listenerMap.get(viewClass);
    if (listener != null) {
      listener.accept(controller);
    }

    // Apply the view configurator, if provided
    if (viewConfigurator != null) {
      viewConfigurator.accept(controller);
    }
  }

  private void checkForViewAnnotation(Class<?> viewClass) {
    if (!viewClass.isAnnotationPresent(View.class)) {
      throw new IllegalArgumentException("Class " + viewClass.getName() + " is not a view.");
    }
  }
}
