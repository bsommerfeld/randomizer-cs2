package de.bsommerfeld.model.action.repository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.bsommerfeld.model.action.sequence.ActionSequence;
import de.bsommerfeld.model.action.spi.ActionSequenceRepository;
import de.bsommerfeld.model.persistence.dao.ActionSequenceDao;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Default implementation of the ActionSequenceRepository interface.
 * This class manages action sequences and provides caching functionality.
 */
@Slf4j
@Singleton
public class DefaultActionSequenceRepository implements ActionSequenceRepository {

    private final Map<String, ActionSequence> actionSequencesMap = new HashMap<>();
    private final ActionSequenceDao actionSequenceDao;
    private boolean isCacheUpdated = false;

    @Inject
    public DefaultActionSequenceRepository(ActionSequenceDao actionSequenceDao) {
        this.actionSequenceDao = actionSequenceDao;
    }

    /**
     * Saves an action sequence to the storage and updates the cache.
     *
     * @param actionSequence The action sequence to be saved. It must not be null.
     * @throws NullPointerException if the action sequence is null
     */
    @Override
    public synchronized void saveActionSequence(ActionSequence actionSequence) {
        Objects.requireNonNull(actionSequence, "ActionSequence must not be null");

        if (actionSequencesMap.containsKey(actionSequence.getName())) {
            log.warn(
                    "ActionSequence with name '{}' already exists and will be overwritten.",
                    actionSequence.getName());
        }

        actionSequencesMap.put(actionSequence.getName(), actionSequence);
        isCacheUpdated = false;
        actionSequenceDao.saveActionSequence(actionSequence);
        log.info("ActionSequence '{}' saved.", actionSequence.getName());
    }

    /**
     * Deletes the given action sequence from the system.
     *
     * @param actionSequence the action sequence to be deleted; must not be null
     * @throws NullPointerException if the action sequence is null
     */
    @Override
    public synchronized void deleteActionSequence(ActionSequence actionSequence) {
        Objects.requireNonNull(actionSequence, "ActionSequence must not be null");

        if (!actionSequencesMap.containsKey(actionSequence.getName())) {
            log.warn(
                    "Attempted to delete a non-existent ActionSequence: {}",
                    actionSequence.getName());
            return;
        }

        removeActionSequence(actionSequence.getName());
        isCacheUpdated = false;
        actionSequenceDao.deleteActionSequence(actionSequence);
        log.info("ActionSequence '{}' deleted.", actionSequence.getName());
    }

    /**
     * Adds an ActionSequence to the internal storage, ensuring no duplicate entries.
     *
     * @param actionSequence the ActionSequence to be added; must not be null
     * @throws NullPointerException if the action sequence is null
     */
    @Override
    public synchronized void addActionSequence(ActionSequence actionSequence) {
        Objects.requireNonNull(actionSequence, "ActionSequence must not be null");

        if (actionSequencesMap.containsKey(actionSequence.getName())) {
            log.warn(
                    "ActionSequence with name '{}' already exists in storage.",
                    actionSequence.getName());
            return;
        }

        actionSequencesMap.put(actionSequence.getName(), actionSequence);
        log.info("ActionSequence '{}' added.", actionSequence.getName());
    }

    /**
     * Removes an action sequence from the storage map by its name.
     *
     * @param name the name of the action sequence to be removed
     */
    @Override
    public synchronized void removeActionSequence(String name) {
        if (actionSequencesMap.remove(name) == null) {
            log.warn(
                    "No ActionSequence with name '{}' found in storage to remove.",
                    name);
            return;
        }
        log.info("ActionSequence '{}' removed.", name);
    }

    /**
     * Updates the action sequences cache if it is not already updated.
     */
    @Override
    public synchronized void updateActionSequencesCache() {
        if (!isCacheUpdated) {
            updateCache();
            log.info("Cache updated.");
        } else {
            log.info("Cache is already up to date.");
        }
    }

    /**
     * Retrieves the action sequence associated with the given name.
     *
     * @param name the name of the action sequence to retrieve
     * @return an Optional containing the ActionSequence if found, otherwise an empty Optional
     */
    @Override
    public synchronized Optional<ActionSequence> getActionSequence(String name) {
        return Optional.ofNullable(actionSequencesMap.get(name));
    }

    /**
     * Retrieves a list of action sequences from the internal map.
     *
     * @return A list containing all action sequences from the map.
     */
    @Override
    public synchronized List<ActionSequence> getActionSequences() {
        return new ArrayList<>(actionSequencesMap.values());
    }

    /**
     * Updates the action sequences cache by reloading data from the data access object.
     */
    private synchronized void updateCache() {
        actionSequencesMap.clear();
        actionSequenceDao
                .loadActionSequences()
                .forEach(sequence -> actionSequencesMap.put(sequence.getName(), sequence));
        isCacheUpdated = true;
    }
}