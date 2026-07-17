package de.bsommerfeld.randomizer.config.crosshair;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/**
 * The canonical set of crosshair convars the app shows and edits - exactly these, in this order.
 * Each entry carries its random range (used by {@link CrosshairRandomizer}) and a default value
 * (used when a loaded config lacks the key). Adding or removing a convar happens here and nowhere
 * else: display, randomization and the restriction to "only these" all derive from this list.
 *
 * <p>Ranges are chosen so a randomized crosshair stays visible and usable rather than truly
 * arbitrary (e.g. alpha never drops below 150, style stays within the classic family 2-4 that
 * actually respects the other convars). Tune a single line here to change that.
 */
public enum CrosshairConvar {

    DRAW_OUTLINE("cl_crosshair_drawoutline", bool("0")),
    DYNAMIC_MAXDIST_SPLITRATIO("cl_crosshair_dynamic_maxdist_splitratio", decimal(0, 1, 2, "1")),
    DYNAMIC_SPLITALPHA_INNERMOD("cl_crosshair_dynamic_splitalpha_innermod", decimal(0, 1, 2, "0.1")),
    DYNAMIC_SPLITALPHA_OUTERMOD("cl_crosshair_dynamic_splitalpha_outermod", decimal(0, 1, 2, "1")),
    DYNAMIC_SPLITDIST("cl_crosshair_dynamic_splitdist", integer(0, 16, "3")),
    OUTLINE_THICKNESS("cl_crosshair_outlinethickness", decimal(0, 3, 1, "1")),
    T_STYLE("cl_crosshair_t", bool("0")),
    ALPHA("cl_crosshairalpha", integer(150, 255, "255")),
    COLOR("cl_crosshaircolor", integer(0, 5, "5")),
    COLOR_B("cl_crosshaircolor_b", integer(0, 255, "255")),
    COLOR_G("cl_crosshaircolor_g", integer(0, 255, "255")),
    COLOR_R("cl_crosshaircolor_r", integer(0, 255, "255")),
    DOT("cl_crosshairdot", bool("0")),
    GAP("cl_crosshairgap", integer(-4, 2, "-4")),
    GAP_USEWEAPONVALUE("cl_crosshairgap_useweaponvalue", bool("0")),
    SIZE("cl_crosshairsize", decimal(1, 4, 1, "1")),
    STYLE("cl_crosshairstyle", integer(2, 4, "4")),
    THICKNESS("cl_crosshairthickness", decimal(0.5, 2.5, 1, "1")),
    USE_ALPHA("cl_crosshairusealpha", bool("0")),
    FIXED_GAP("cl_fixedcrosshairgap", integer(-4, 4, "3")),
    RECOIL("cl_crosshair_recoil", bool("0"));

    private final String key;
    private final ValueSpec spec;

    CrosshairConvar(String key, ValueSpec spec) {
        this.key = key;
        this.spec = spec;
    }

    /** The convar name, e.g. {@code cl_crosshairsize}. */
    public String key() {
        return key;
    }

    /** A fresh random value within this convar's range. */
    public String randomValue(Random random) {
        return spec.random(random);
    }

    /** The default value used when a loaded config does not contain this convar. */
    public String defaultValue() {
        return spec.defaultValue();
    }

    /** The default value for every convar, in canonical order. */
    public static Map<String, String> defaults() {
        Map<String, String> values = new LinkedHashMap<>();
        for (CrosshairConvar convar : values()) {
            values.put(convar.key, convar.spec.defaultValue());
        }
        return values;
    }

    /**
     * Restricts {@code source} to exactly the canonical convars, in canonical order: known keys keep
     * their value, missing keys fall back to their default, and any other key in {@code source} is
     * dropped. This is what enforces "only these values are shown and changed".
     */
    public static Map<String, String> canonicalValues(Map<String, String> source) {
        Map<String, String> values = new LinkedHashMap<>();
        for (CrosshairConvar convar : values()) {
            values.put(convar.key, source.getOrDefault(convar.key, convar.spec.defaultValue()));
        }
        return values;
    }

    // ---- value specs -----------------------------------------------------------------------

    private interface ValueSpec {
        String random(Random random);

        String defaultValue();
    }

    private static ValueSpec bool(String def) {
        return new BoolSpec(def);
    }

    private static ValueSpec integer(int min, int max, String def) {
        return new IntSpec(min, max, def);
    }

    private static ValueSpec decimal(double min, double max, int decimals, String def) {
        return new DecimalSpec(min, max, decimals, def);
    }

    private record BoolSpec(String def) implements ValueSpec {
        @Override
        public String random(Random random) {
            return random.nextBoolean() ? "1" : "0";
        }

        @Override
        public String defaultValue() {
            return def;
        }
    }

    private record IntSpec(int min, int max, String def) implements ValueSpec {
        @Override
        public String random(Random random) {
            return String.valueOf(min + random.nextInt(max - min + 1));
        }

        @Override
        public String defaultValue() {
            return def;
        }
    }

    private record DecimalSpec(double min, double max, int decimals, String def) implements ValueSpec {
        @Override
        public String random(Random random) {
            double value = min + random.nextDouble() * (max - min);
            String text = String.format(Locale.US, "%." + decimals + "f", value);
            // Trim trailing zeros so we write "0.5" / "1" like CS2 does, not "0.50" / "1.0".
            if (text.contains(".")) {
                text = text.replaceAll("0+$", "").replaceAll("\\.$", "");
            }
            return text;
        }

        @Override
        public String defaultValue() {
            return def;
        }
    }
}
