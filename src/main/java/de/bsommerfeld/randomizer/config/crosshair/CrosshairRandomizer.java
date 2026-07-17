package de.bsommerfeld.randomizer.config.crosshair;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

/**
 * Produces a random-but-usable set of values for every {@link CrosshairConvar}. The {@link Random}
 * source is injectable so the outcome can be tested deterministically.
 */
public final class CrosshairRandomizer {

    private final Random random;

    public CrosshairRandomizer() {
        this(new Random());
    }

    public CrosshairRandomizer(Random random) {
        this.random = random;
    }

    /** A fresh value for each canonical convar, in canonical order. */
    public Map<String, String> randomValues() {
        Map<String, String> values = new LinkedHashMap<>();
        for (CrosshairConvar convar : CrosshairConvar.values()) {
            values.put(convar.key(), convar.randomValue(random));
        }
        return values;
    }
}
