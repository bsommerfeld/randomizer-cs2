/**
 * Service Provider Interfaces (SPIs) for the action execution framework.
 *
 * <p>This package defines the core abstractions and contracts for action management, execution, and
 * persistence within the randomizer application. These SPIs enable modular architecture and allow
 * for different implementations to be plugged in at runtime.
 *
 * <p>The primary SPIs include:
 *
 * <ul>
 *   <li>{@link de.bsommerfeld.model.action.spi.ActionExecutor} - Executes individual actions
 *   <li>{@link de.bsommerfeld.model.action.spi.ActionSequenceExecutor} - Handles execution of
 *       action sequences
 *   <li>{@link de.bsommerfeld.model.action.spi.ActionSequenceDispatcher} - Manages and dispatches
 *       action sequences
 *   <li>{@link de.bsommerfeld.model.action.spi.ActionRepository} - Provides CRUD operations for
 *       actions
 *   <li>{@link de.bsommerfeld.model.action.spi.ActionSequenceRepository} - Manages action sequence
 *       persistence
 *   <li>{@link de.bsommerfeld.model.action.spi.FocusManager} - Handles application focus and window
 *       management
 * </ul>
 *
 * <p>These interfaces follow the SPI pattern to enable loose coupling between the core domain logic
 * and specific implementations. Implementations can be swapped or extended without modifying the
 * core business logic.
 *
 * <p><strong>Design Benefits:</strong>
 *
 * <ul>
 *   <li>Testability through mock implementations
 *   <li>Extensibility for new action types
 *   <li>Platform-specific implementations
 *   <li>Clear separation of concerns
 * </ul>
 *
 * @since 1.0
 * @author BSommerfeld
 */
package de.bsommerfeld.model.action.spi;
