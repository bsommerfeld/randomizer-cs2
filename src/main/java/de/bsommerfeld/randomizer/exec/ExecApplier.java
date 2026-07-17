package de.bsommerfeld.randomizer.exec;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Applies convar values to a running CS2: writes them as {@code randomizer.cfg}
 * ({@link ExecConfig}) and triggers {@code exec randomizer} in-game by pressing the bound key on
 * the CS2 window ({@link Cs2Window}). One-time prerequisite in the CS2 console:
 * {@code bind l "exec randomizer"} (key configurable via app.properties, {@link #KEY_PREFERENCE}).
 *
 * <p>Note: {@link #apply} blocks (window focus + key send) - callers off the JavaFX thread only.
 * The key-name supplier is evaluated on every apply, so a changed preference works without an app
 * restart.
 */
public final class ExecApplier {

    /** app.properties key for the exec-trigger key name ({@code f1}-{@code f24}, {@code a}-{@code z}). */
    public static final String KEY_PREFERENCE = "cs2.exec.key";
    public static final String DEFAULT_KEY = "l";

    public enum Result {
        /** cfg written and the exec key pressed in a focused CS2 - values are live. */
        TRIGGERED,
        /** cfg written; no CS2 window - values apply on the next start. */
        CFG_ONLY_WINDOW_NOT_FOUND,
        /** cfg written; CS2 would not come to the foreground - exec manually in-game. */
        CFG_ONLY_FOCUS_DENIED,
        /** Nothing written: CS2's cfg folder could not be resolved. */
        CFG_FOLDER_MISSING
    }

    private final ExecConfig config;
    private final Cs2Window window;
    private final Supplier<String> key;

    public ExecApplier(ExecConfig config, Cs2Window window, Supplier<String> key) {
        this.config = config;
        this.window = window;
        this.key = key;
    }

    /** Writes the cfg and presses the exec key; see {@link Result} for the outcomes. */
    public Result apply(Map<String, String> values) throws IOException {
        if (config.write(values).isEmpty()) {
            return Result.CFG_FOLDER_MISSING;
        }
        ExecKeys.Key execKey = ExecKeys.key(key.get())
                .orElseGet(() -> ExecKeys.key(DEFAULT_KEY).orElseThrow());
        return switch (window.pressKey(execKey)) {
            case PRESSED -> Result.TRIGGERED;
            case WINDOW_NOT_FOUND -> Result.CFG_ONLY_WINDOW_NOT_FOUND;
            case FOCUS_DENIED -> Result.CFG_ONLY_FOCUS_DENIED;
        };
    }

    /** The effective key name (configured value if valid, else {@link #DEFAULT_KEY}) for status texts. */
    public String keyName() {
        String configured = key.get();
        return ExecKeys.key(configured).isPresent()
                ? configured.trim().toLowerCase(Locale.ROOT)
                : DEFAULT_KEY;
    }
}
