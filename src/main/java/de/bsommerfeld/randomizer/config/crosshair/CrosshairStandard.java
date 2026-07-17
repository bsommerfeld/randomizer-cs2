package de.bsommerfeld.randomizer.config.crosshair;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

/**
 * Resolves the user's "standard" crosshair - the values the reset button restores to - and keeps its
 * on-disk backup ({@link CrosshairBackup}) current.
 *
 * <p>The standard is the user's genuine crosshair, never the randomized/edited working copy: it is
 * backed up the first time a crosshair is loaded, or whenever the user picks a file explicitly, and
 * is otherwise left untouched (so an auto-load never clobbers it). When no file is loaded the
 * existing backup stands in; only with neither file nor backup do CS2 defaults apply.
 */
public final class CrosshairStandard {

    private final CrosshairBackup backup;

    public CrosshairStandard(CrosshairBackup backup) {
        this.backup = backup;
    }

    /**
     * The standard crosshair's canonical values for this load result. Side effect: backs the loaded
     * crosshair up when appropriate (first sight, or an explicit file choice).
     *
     * @param loaded   the freshly loaded crosshair, or empty if none was found
     * @param explicit whether the load came from the user explicitly picking a file
     */
    public Map<String, String> resolve(Optional<Crosshair> loaded, boolean explicit) {
        if (loaded.isPresent()) {
            Crosshair crosshair = loaded.get();
            if (explicit || !backup.exists()) {
                trySave(crosshair.source());
            }
            return backup.read()
                    .map(saved -> CrosshairConvar.canonicalValues(saved.convars()))
                    .orElseGet(() -> CrosshairConvar.canonicalValues(crosshair.convars()));
        }
        return backup.read()
                .map(saved -> CrosshairConvar.canonicalValues(saved.convars()))
                .orElseGet(CrosshairConvar::defaults);
    }

    /** Whether a backup of the user's standard crosshair exists on disk. */
    public boolean hasBackup() {
        return backup.exists();
    }

    /** Restores the standard backup byte-exactly over the live file at {@code target}. */
    public void restoreTo(Path target) throws IOException {
        backup.restoreTo(target);
    }

    private void trySave(Path source) {
        try {
            backup.save(source);
        } catch (IOException e) {
            // Best-effort: a failed backup must not break loading. Reset then falls back to the
            // freshly loaded values for this session.
        }
    }
}
