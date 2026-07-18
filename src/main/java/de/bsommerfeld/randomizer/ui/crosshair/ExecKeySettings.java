package de.bsommerfeld.randomizer.ui.crosshair;

import de.bsommerfeld.randomizer.config.AppPreferences;
import de.bsommerfeld.randomizer.exec.ExecApplier;
import de.bsommerfeld.randomizer.exec.ExecConfig;
import de.bsommerfeld.randomizer.exec.ExecKeys;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.util.Locale;

/**
 * The exec-key settings row: validates and persists the key that triggers {@code exec randomizer}
 * ({@link ExecApplier#KEY_PREFERENCE}) as the user types, and keeps the copyable bind command in
 * sync with the effective key.
 */
final class ExecKeySettings {

    private final TextField keyField;
    private final Label hint;
    private final TextField bindCommandField;
    private final AppPreferences preferences;
    private final ExecApplier execApplier;

    ExecKeySettings(TextField keyField, Label hint, TextField bindCommandField,
                    AppPreferences preferences, ExecApplier execApplier) {
        this.keyField = keyField;
        this.hint = hint;
        this.bindCommandField = bindCommandField;
        this.preferences = preferences;
        this.execApplier = execApplier;
    }

    /** Shows the current key and bind command and starts reacting to edits. */
    void init() {
        keyField.setText(execApplier.keyName());
        updateBindCommand();
        keyField.textProperty().addListener((observable, oldText, newText) -> onKeyChanged(newText));
    }

    private void onKeyChanged(String text) {
        if (ExecKeys.key(text).isEmpty()) {
            hint.setText("Ungültig - erlaubt: a-z, f1-f24");
            return;
        }
        try {
            preferences.setString(ExecApplier.KEY_PREFERENCE, text.trim().toLowerCase(Locale.ROOT));
            hint.setText("");
            updateBindCommand();
        } catch (IOException e) {
            hint.setText("Speichern fehlgeschlagen: " + e.getMessage());
        }
    }

    /** The console command matching the configured key, ready to copy into CS2. */
    private void updateBindCommand() {
        bindCommandField.setText("bind " + execApplier.keyName()
                + " \"exec " + ExecConfig.EXEC_NAME + "\"");
    }
}
