package de.bsommerfeld.model.persistence.dao;

import com.google.inject.Inject;
import de.bsommerfeld.model.ApplicationContext;
import de.bsommerfeld.model.action.sequence.ActionSequence;
import de.bsommerfeld.model.persistence.JsonUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
public class ActionSequenceDao {

    public static final File ACTION_SEQUENCE_FOLDER =
            new File(ApplicationContext.getAppdataFolder() + File.separator + "sequences");

    static {
        if (!ACTION_SEQUENCE_FOLDER.exists() && !ACTION_SEQUENCE_FOLDER.mkdirs()) {
            log.error(
                    "Fehler beim Erstellen des actionSequence-Ordners: {}",
                    ACTION_SEQUENCE_FOLDER.getAbsolutePath());
        }
    }

    @Inject
    JsonUtil jsonUtil;

    /**
     * Saves the given ActionSequence to a file in the specified folder. The ActionSequence object is serialized to JSON
     * format before being saved.
     *
     * @param actionSequence the ActionSequence object to be saved. Must not be null.
     */
    public synchronized void saveActionSequence(ActionSequence actionSequence) {
        Objects.requireNonNull(actionSequence, "ActionSequence darf nicht null sein");

        File file = new File(ACTION_SEQUENCE_FOLDER, actionSequence.getName() + ".sequence");
        writeActionSequenceToFile(actionSequence, file);
    }

    private void writeActionSequenceToFile(ActionSequence actionSequence, File file) {
        try (PrintWriter writer = new PrintWriter(file)) {
            String json = jsonUtil.serialize(actionSequence);
            writer.println(json);
            writer.flush();
            log.info("ActionSequence erfolgreich gespeichert: {}", actionSequence.getName());
        } catch (IOException e) {
            log.error("Fehler beim Speichern der ActionSequence: {}", actionSequence.getName(), e);
        }
    }

    /**
     * Deletes the specified ActionSequence from the filesystem.
     *
     * @param actionSequence The ActionSequence to be deleted. Must not be null.
     *
     * @throws NullPointerException if the provided actionSequence is null.
     * @throws RuntimeException     if there is an IOException during the deletion process.
     */
    public synchronized void deleteActionSequence(ActionSequence actionSequence) {
        Objects.requireNonNull(actionSequence, "ActionSequence darf nicht null sein");

        File file = new File(ACTION_SEQUENCE_FOLDER, actionSequence.getName() + ".sequence");
        if (!file.exists()) {
            log.warn("ActionSequence-Datei existiert nicht: {}", file.getAbsolutePath());
            return;
        }

        try {
            Files.delete(file.toPath());
            log.info("ActionSequence erfolgreich gelöscht: {}", actionSequence.getName());
        } catch (IOException e) {
            log.error("Fehler beim Löschen der ActionSequence: {}", actionSequence.getName(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Loads all action sequences from the specified folder.
     *
     * @return a list of {@link ActionSequence} objects loaded from the folder. If no files are found or an error occurs
     * during the load, an empty list will be returned.
     */
    public List<ActionSequence> loadActionSequences() {
        List<ActionSequence> actionSequences = new ArrayList<>();
        File[] actionSequenceFiles = ACTION_SEQUENCE_FOLDER.listFiles();

        if (areFilesEmpty(actionSequenceFiles)) {
            log.warn(
                    "Keine ActionSequence-Dateien gefunden im Ordner: {}",
                    ACTION_SEQUENCE_FOLDER.getAbsolutePath());
            return actionSequences;
        }

        for (File file : actionSequenceFiles) {
            if (isSequenceFile(file)) {
                ActionSequence actionSequence = null;
                try {
                    actionSequence = loadActionSequenceFromFile(file);
                } catch (IOException ignore) {
                }

                if (actionSequence != null) {
                    actionSequences.add(actionSequence);
                }
            }
        }

        log.info("Insgesamt {} ActionSequences erfolgreich geladen", actionSequences.size());
        return actionSequences;
    }

    private boolean areFilesEmpty(File[] files) {
        return files == null || files.length == 0;
    }

    private boolean isSequenceFile(File file) {
        return file.getName().endsWith(".sequence");
    }

    private ActionSequence loadActionSequenceFromFile(File file) throws IOException {
        try {
            String content = new String(Files.readAllBytes(file.toPath()));
            return jsonUtil.deserializeActionSequence(content);
        } catch (IOException e) {
            log.error("Fehler beim Laden der ActionSequence aus Datei: {}", file.getAbsolutePath(), e);
            return null;
        }
    }
}
