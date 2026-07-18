package de.bsommerfeld.randomizer.ui.crosshair;

import de.bsommerfeld.randomizer.exec.ExecApplier;
import de.bsommerfeld.randomizer.exec.ExecConfig;
import javafx.application.Platform;

import java.io.IOException;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Applies convar values to a running CS2 in the background: runs {@link ExecApplier} on a virtual
 * thread (it blocks on window focus + key send) and translates every {@link ExecApplier.Result}
 * into a user-facing status text, delivered on the JavaFX thread. Never fails the calling action.
 * Whether a delivered text is still worth showing is the caller's decision - a newer status may
 * exist by then.
 */
final class LiveApplier {

    private final ExecApplier execApplier;

    LiveApplier(ExecApplier execApplier) {
        this.execApplier = execApplier;
    }

    /**
     * Applies a snapshot of {@code values} asynchronously; the outcome text (starting with
     * {@code prefix}) goes to {@code onStatus} on the JavaFX thread.
     */
    void apply(Map<String, String> values, String prefix, Consumer<String> onStatus) {
        Map<String, String> snapshot = Map.copyOf(values);
        Thread.startVirtualThread(() -> {
            String status = statusText(snapshot, prefix);
            Platform.runLater(() -> onStatus.accept(status));
        });
    }

    private String statusText(Map<String, String> values, String prefix) {
        try {
            return switch (execApplier.apply(values)) {
                case TRIGGERED -> prefix + " und live an CS2 übertragen (exec "
                        + ExecConfig.EXEC_NAME + " per " + execApplier.keyName() + ").";
                case CFG_ONLY_WINDOW_NOT_FOUND ->
                        prefix + ". CS2 läuft nicht - Werte gelten ab dem nächsten Start.";
                case CFG_ONLY_FOCUS_DENIED -> prefix + ". Automatischer exec fehlgeschlagen - im"
                        + " Spiel \"exec " + ExecConfig.EXEC_NAME + "\" ausführen (Bind: bind "
                        + execApplier.keyName() + " \"exec " + ExecConfig.EXEC_NAME + "\").";
                case CFG_FOLDER_MISSING -> prefix
                        + " - CS2-cfg-Ordner nicht gefunden; Werte gelten ab dem nächsten Start.";
            };
        } catch (IOException e) {
            return prefix + " - " + ExecConfig.FILE_NAME
                    + " konnte nicht geschrieben werden: " + e.getMessage();
        }
    }
}
