package de.bsommerfeld.model.action.spi;

import de.bsommerfeld.model.action.sequence.ActionSequence;

import java.util.List;
import java.util.Optional;

/**
 * Interface for repositories that manage action sequences.
 * This interface defines the contract for classes that store and retrieve action sequences.
 */
public interface ActionSequenceRepository {

    /**
     * Saves an action sequence to the storage and updates the cache.
     *
     * @param actionSequence The action sequence to be saved. It must not be null.
     * @throws NullPointerException if the action sequence is null
     */
    void saveActionSequence(ActionSequence actionSequence);

    /**
     * Deletes the given action sequence from the system.
     *
     * @param actionSequence the action sequence to be deleted; must not be null
     * @throws NullPointerException if the action sequence is null
     */
    void deleteActionSequence(ActionSequence actionSequence);

    /**
     * Adds an ActionSequence to the internal storage, ensuring no duplicate entries.
     *
     * @param actionSequence the ActionSequence to be added; must not be null
     * @throws NullPointerException if the action sequence is null
     */
    void addActionSequence(ActionSequence actionSequence);

    /**
     * Removes an action sequence from the storage map by its name.
     *
     * @param name the name of the action sequence to be removed
     */
    void removeActionSequence(String name);

    /**
     * Updates the action sequences cache if it is not already updated.
     */
    void updateActionSequencesCache();

    /**
     * Retrieves the action sequence associated with the given name.
     *
     * @param name the name of the action sequence to retrieve
     * @return an Optional containing the ActionSequence if found, otherwise an empty Optional
     */
    Optional<ActionSequence> getActionSequence(String name);

    /**
     * Retrieves a list of action sequences from the internal map.
     *
     * @return A list containing all action sequences from the map.
     */
    List<ActionSequence> getActionSequences();
}