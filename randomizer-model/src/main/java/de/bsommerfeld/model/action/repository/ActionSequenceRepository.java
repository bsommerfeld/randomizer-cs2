package de.bsommerfeld.model.action.repository;

import com.google.inject.Inject;
import de.bsommerfeld.model.action.sequence.ActionSequence;
import de.bsommerfeld.model.persistence.dao.ActionSequenceDao;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Slf4j
public class ActionSequenceRepository {

    private final Map<String, ActionSequence> actionSequencesMap = new HashMap<>();
    private final ActionSequenceDao actionSequenceDao;
    private boolean isCacheUpdated = false;

    @Inject
    public ActionSequenceRepository(ActionSequenceDao actionSequenceDao) {
        this.actionSequenceDao = actionSequenceDao;
    }

    /**
     * Saves an action sequence to the storage and updates the cache.
     *
     * @param actionSequence The action sequence to be saved. It must not be null.
     */
    public synchronized void saveActionSequence(ActionSequence actionSequence) {
        Objects.requireNonNull(actionSequence, "ActionSequence darf nicht null sein");

        if (actionSequencesMap.containsKey(actionSequence.getName())) {
            log.warn(
                    "ActionSequence mit dem Namen '{}' existiert bereits und wird überschrieben.",
                    actionSequence.getName());
        }

        actionSequencesMap.put(actionSequence.getName(), actionSequence);
        isCacheUpdated = false;
        actionSequenceDao.saveActionSequence(actionSequence);
        log.info("ActionSequence '{}' gespeichert.", actionSequence.getName());
    }

    /**
     * Deletes the given action sequence from the system.
     *
     * @param actionSequence the action sequence to be deleted; must not be null
     *
     * @throws NullPointerException     if the action sequence is null
     * @throws IllegalArgumentException if the action sequence does not exist
     */
    public synchronized void deleteActionSequence(ActionSequence actionSequence) {
        Objects.requireNonNull(actionSequence, "ActionSequence darf nicht null sein");

        if (!actionSequencesMap.containsKey(actionSequence.getName())) {
            log.warn(
                    "Versucht, eine nicht existierende ActionSequence zu löschen: {}",
                    actionSequence.getName());
            return;
        }

        removeActionSequence(actionSequence.getName());
        isCacheUpdated = false;
        actionSequenceDao.deleteActionSequence(actionSequence);
        log.info("ActionSequence '{}' gelöscht.", actionSequence.getName());
    }

    /**
     * Adds an ActionSequence to the internal storage, ensuring no duplicate entries.
     *
     * @param actionSequence the ActionSequence to be added; must not be null
     */
    public synchronized void addActionSequence(ActionSequence actionSequence) {
        Objects.requireNonNull(actionSequence, "ActionSequence darf nicht null sein");

        if (actionSequencesMap.containsKey(actionSequence.getName())) {
            log.warn(
                    "ActionSequence mit dem Namen '{}' existiert bereits im Speicher.",
                    actionSequence.getName());
            return;
        }

        actionSequencesMap.put(actionSequence.getName(), actionSequence);
        log.info("ActionSequence '{}' hinzugefügt.", actionSequence.getName());
    }

    /**
     * Removes an action sequence from the storage map by its name.
     *
     * @param name the name of the action sequence to be removed
     */
    public synchronized void removeActionSequence(String name) {
        if (actionSequencesMap.remove(name) == null) {
            log.warn(
                    "Keine ActionSequence mit dem Namen '{}' im Speicher gefunden, um sie zu entfernen.",
                    name);
            return;
        }
        log.info("ActionSequence '{}' entfernt.", name);
    }

    /**
     * Updates the action sequences cache if it is not already updated. This method is synchronized to prevent
     * concurrent updates. If the cache is updated, it will log that the cache is up to date. If the cache is not
     * updated, it will update the cache and log the update.
     */
    public synchronized void updateActionSequencesCache() {
        if (!isCacheUpdated) {
            updateCache();
            log.info("Cache aktualisiert.");
        } else {
            log.info("Cache ist aktuell.");
        }
    }

    /**
     * Retrieves the action sequence associated with the given name.
     *
     * @param name the name of the action sequence to retrieve
     *
     * @return an Optional containing the ActionSequence if found, otherwise an empty Optional
     */
    public synchronized Optional<ActionSequence> getActionSequence(String name) {
        return Optional.ofNullable(actionSequencesMap.get(name));
    }

    /**
     * Retrieves a synchronized list of action sequences from the internal map.
     *
     * @return A synchronized list containing all action sequences from the map.
     */
    public synchronized List<ActionSequence> getActionSequences() {
        return new ArrayList<>(actionSequencesMap.values());
    }

    /**
     * Updates the action sequences cache by reloading data from the data access object. The method clears the existing
     * cache and loads the latest action sequences, then updates the cache map and sets the cache updated flag to true.
     *
     * <p>This method is synchronized to ensure thread safety during the update process.
     */
    private synchronized void updateCache() {
        actionSequencesMap.clear();
        actionSequenceDao
                .loadActionSequences()
                .forEach(sequence -> actionSequencesMap.put(sequence.getName(), sequence));
        isCacheUpdated = true;
    }
}
