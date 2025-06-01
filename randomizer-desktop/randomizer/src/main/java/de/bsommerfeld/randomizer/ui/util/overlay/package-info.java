/**
 * Overlay utility package for creating floating UI overlays in JavaFX applications.
 *
 * <h2>Overview</h2>
 *
 * This package provides a robust solution for creating overlay components that float above existing
 * UI elements without modifying the underlying scene graph structure. The implementation uses
 * JavaFX {@link javafx.stage.Popup} for true layering and automatic z-index management.
 *
 * <h2>Key Features</h2>
 *
 * <ul>
 *   <li><strong>True Floating Overlays:</strong> Uses Popup for authentic overlay behavior
 *   <li><strong>Automatic Positioning:</strong> Tracks target container bounds and updates position
 *       dynamically
 *   <li><strong>Lifecycle Management:</strong> Automatic cleanup and dissolving based on visibility
 *       changes
 *   <li><strong>Smooth Animations:</strong> Built-in fade-in/fade-out transitions
 *   <li><strong>Memory Safe:</strong> Automatic listener cleanup prevents memory leaks
 * </ul>
 *
 * <h2>Architecture</h2>
 *
 * <h3>Core Components</h3>
 *
 * <pre>
 * ┌─────────────────┐
 * │     Popup       │ ← OS-level window overlay
 * │  ┌───────────┐  │
 * │  │ StackPane │  │ ← Wrapper with background
 * │  │  ┌─────┐  │  │
 * │  │  │User │  │  │ ← Actual overlay content
 * │  │  │Node │  │  │
 * │  │  └─────┘  │  │
 * │  └───────────┘  │
 * └─────────────────┘
 * </pre>
 *
 * <h3>Positioning System</h3>
 *
 * The overlay positioning works through a coordinate transformation chain:
 *
 * <ol>
 *   <li>Target container local bounds → screen coordinates via {@code localToScreen()}
 *   <li>Popup positioned at calculated screen coordinates
 *   <li>Bounds listeners track changes and update position automatically
 * </ol>
 *
 * <h2>Usage Patterns</h2>
 *
 * <h3>Basic Overlay Creation</h3>
 *
 * <pre>{@code
 * // Create overlay content
 * VBox settingsMenu = new VBox();
 * settingsMenu.getChildren().addAll(
 *     new Label("Settings"),
 *     new Button("Option 1"),
 *     new Button("Option 2")
 * );
 *
 * // Create overlay over main container
 * Overlay overlay = Overlay.overlay(settingsMenu, mainContainer);
 *
 * // Manually close when needed
 * overlay.dissolve();
 * }</pre>
 *
 * <h3>Event-Driven Overlay</h3>
 *
 * <pre>{@code
 * settingsButton.setOnAction(e -> {
 *     VBox menu = createSettingsMenu();
 *     Overlay overlay = Overlay.overlay(menu, targetContainer);
 *
 *     // Add close button to menu
 *     Button closeBtn = new Button("Close");
 *     closeBtn.setOnAction(evt -> overlay.dissolve());
 *     menu.getChildren().add(closeBtn);
 * });
 * }</pre>
 *
 * <h3>Status Monitoring</h3>
 *
 * <pre>{@code
 * Overlay overlay = Overlay.overlay(content, container);
 *
 * // React to overlay state changes
 * overlay.dissolvedProperty().addListener((obs, oldVal, newVal) -> {
 *     if (newVal) {
 *         System.out.println("Overlay closed");
 *         // Perform cleanup actions
 *     }
 * });
 * }</pre>
 *
 * <h2>Automatic Lifecycle Management</h2>
 *
 * <h3>Auto-Dissolve Triggers</h3>
 *
 * The overlay automatically dissolves when any of these conditions occur:
 *
 * <ul>
 *   <li><strong>Content Visibility:</strong> {@code overlayContent.setVisible(false)}
 *   <li><strong>Content Management:</strong> {@code overlayContent.setManaged(false)}
 *   <li><strong>Target Visibility:</strong> Target container becomes invisible
 *   <li><strong>Scene Removal:</strong> Target container removed from scene graph
 *   <li><strong>Window Close:</strong> Parent window is closed
 * </ul>
 *
 * <h3>Position Tracking</h3>
 *
 * The overlay automatically tracks these target container changes:
 *
 * <ul>
 *   <li>{@code boundsInLocalProperty()} - Position and size changes
 *   <li>{@code layoutBoundsProperty()} - Layout-based position changes
 * </ul>
 *
 * <h2>Animation System</h2>
 *
 * <h3>Built-in Animations</h3>
 *
 * <table border="1">
 *   <tr><th>Animation</th><th>Duration</th><th>Property</th><th>Range</th></tr>
 *   <tr><td>Fade In</td><td>200ms</td><td>opacity</td><td>0.0 → 1.0</td></tr>
 *   <tr><td>Fade Out</td><td>200ms</td><td>opacity</td><td>1.0 → 0.0</td></tr>
 * </table>
 *
 * <h3>Custom Animation Extension</h3>
 *
 * <pre>{@code
 * Overlay overlay = Overlay.overlay(content, container);
 * StackPane wrapper = (StackPane) overlay.getPopup().getContent().get(0);
 *
 * // Add custom scale animation
 * ScaleTransition scale = new ScaleTransition(Duration.millis(300), wrapper);
 * scale.setFromX(0.8);
 * scale.setToX(1.0);
 * scale.play();
 * }</pre>
 *
 * <h2>Styling and Theming</h2>
 *
 * <h3>Default Styling</h3>
 *
 * The overlay wrapper uses a semi-transparent dark background:
 *
 * <pre>{@code
 * -fx-background-color: rgba(0, 0, 0, 0.3);
 * }</pre>
 *
 * <h3>Custom Styling</h3>
 *
 * <pre>{@code
 * Overlay overlay = Overlay.overlay(content, container);
 * StackPane wrapper = (StackPane) overlay.getPopup().getContent().get(0);
 * wrapper.setStyle("-fx-background-color: rgba(255, 255, 255, 0.9);");
 * }</pre>
 *
 * <h2>Advanced Configuration</h2>
 *
 * <h3>Popup Behavior Modification</h3>
 *
 * <pre>{@code
 * Overlay overlay = Overlay.overlay(content, container);
 * Popup popup = overlay.getPopup();
 *
 * // Enable click-outside-to-close
 * popup.setAutoHide(true);
 *
 * // Make modal-like (consumes all events)
 * popup.setConsumeAutoHidingEvents(true);
 * }</pre>
 *
 * <h2>Performance Considerations</h2>
 *
 * <h3>Memory Management</h3>
 *
 * <ul>
 *   <li><strong>Automatic Cleanup:</strong> All listeners are removed on dissolve
 *   <li><strong>Popup Disposal:</strong> Native popup resources are freed
 *   <li><strong>Weak References:</strong> Consider using weak references for long-lived overlays
 * </ul>
 *
 * <h3>Performance Optimization</h3>
 *
 * <ul>
 *   <li><strong>Bounds Caching:</strong> Position updates are throttled to layout cycles
 *   <li><strong>Hardware Acceleration:</strong> Animations use JavaFX Timeline (GPU accelerated)
 *   <li><strong>Minimal Reflow:</strong> No modification of existing scene graph
 * </ul>
 *
 * <h2>Threading Model</h2>
 *
 * <h3>Thread Safety</h3>
 *
 * <strong>Important:</strong> This package is NOT thread-safe. All operations must be performed on
 * the JavaFX Application Thread.
 *
 * <pre>{@code
 * // Correct usage from background thread
 * Platform.runLater(() -> {
 *     Overlay overlay = Overlay.overlay(content, container);
 * });
 * }</pre>
 *
 * <h2>Error Handling</h2>
 *
 * <h3>Common Exceptions</h3>
 *
 * <ul>
 *   <li><strong>IllegalStateException:</strong> Target container not in scene graph
 *   <li><strong>NullPointerException:</strong> Null parameters or disposed components
 * </ul>
 *
 * <h3>Defensive Programming</h3>
 *
 * <pre>{@code
 * try {
 *     Overlay overlay = Overlay.overlay(content, container);
 * } catch (IllegalStateException e) {
 *     // Handle case where container is not properly initialized
 *     logger.warn("Cannot create overlay: container not in scene", e);
 * }
 * }</pre>
 *
 * <h2>Limitations and Constraints</h2>
 *
 * <h3>Technical Limitations</h3>
 *
 * <ul>
 *   <li><strong>Scene Requirement:</strong> Target container must be in a displayed scene
 *   <li><strong>Window Binding:</strong> Overlay is bound to the parent window
 *   <li><strong>Screen Coordinates:</strong> Limited to primary screen of parent window
 *   <li><strong>Z-Index:</strong> Cannot appear above other application windows
 * </ul>
 *
 * <h3>Browser/WebView Considerations</h3>
 *
 * When used in JavaFX WebView contexts, overlays may have reduced functionality due to browser
 * security restrictions.
 *
 * <h2>Testing Considerations</h2>
 *
 * <h3>Unit Testing</h3>
 *
 * <pre>{@code
 * // Mock scene hierarchy for testing
 * @Test
 * public void testOverlayCreation() {
 *     // Setup
 *     StackPane container = new StackPane();
 *     Scene scene = new Scene(container, 400, 300);
 *     Stage stage = new Stage();
 *     stage.setScene(scene);
 *     stage.show();
 *
 *     // Test
 *     Label content = new Label("Test");
 *     Overlay overlay = Overlay.overlay(content, container);
 *
 *     // Verify
 *     assertFalse(overlay.isDissolved());
 *     assertEquals(content, overlay.getOverlayContent());
 * }
 * }</pre>
 *
 * <h2>Migration Guide</h2>
 *
 * <h3>From Manual Popup Management</h3>
 *
 * <pre>{@code
 * // Old approach
 * Popup popup = new Popup();
 * popup.getContent().add(content);
 * popup.show(stage);
 * // Manual positioning and cleanup required
 *
 * // New approach
 * Overlay overlay = Overlay.overlay(content, container);
 * // Automatic positioning and cleanup
 * }</pre>
 *
 * <h3>From Scene Graph Modification</h3>
 *
 * <pre>{@code
 * // Old approach - modifies scene graph
 * container.getChildren().add(overlayContent);
 *
 * // New approach - true overlay
 * Overlay overlay = Overlay.overlay(overlayContent, container);
 * }</pre>
 *
 * @since 1.0
 * @author AI Assistant
 * @see javafx.stage.Popup
 * @see javafx.animation.FadeTransition
 * @see javafx.beans.property.BooleanProperty
 */
package de.bsommerfeld.randomizer.ui.util.overlay;
