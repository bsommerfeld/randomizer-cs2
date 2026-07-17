package de.bsommerfeld.randomizer.config.crosshair;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrosshairRandomizerTest {

    @Test
    void producesAValueForEveryCanonicalConvar() {
        Map<String, String> values = new CrosshairRandomizer(new Random(1)).randomValues();

        assertEquals(CrosshairConvar.values().length, values.size());
        for (CrosshairConvar convar : CrosshairConvar.values()) {
            assertTrue(values.containsKey(convar.key()), "value for " + convar.key());
        }
    }

    @Test
    void isDeterministicForAGivenSeed() {
        assertEquals(new CrosshairRandomizer(new Random(42)).randomValues(),
                new CrosshairRandomizer(new Random(42)).randomValues());
    }

    /** Runs many seeds so the range assertions actually exercise the spread. */
    @RepeatedTest(50)
    void randomValuesStayWithinTheirRanges() {
        Map<String, String> v = new CrosshairRandomizer(new Random()).randomValues();

        assertBool(v, "cl_crosshair_drawoutline");
        assertBool(v, "cl_crosshair_t");
        assertBool(v, "cl_crosshairdot");
        assertBool(v, "cl_crosshairgap_useweaponvalue");
        assertBool(v, "cl_crosshairusealpha");
        assertBool(v, "cl_crosshair_recoil");

        assertIntInRange(v, "cl_crosshairstyle", 2, 4);
        assertIntInRange(v, "cl_crosshaircolor", 0, 5);
        assertIntInRange(v, "cl_crosshaircolor_r", 0, 255);
        assertIntInRange(v, "cl_crosshaircolor_g", 0, 255);
        assertIntInRange(v, "cl_crosshaircolor_b", 0, 255);
        assertIntInRange(v, "cl_crosshairalpha", 150, 255);
        assertIntInRange(v, "cl_crosshairgap", -4, 2);
        assertIntInRange(v, "cl_fixedcrosshairgap", -4, 4);
        assertIntInRange(v, "cl_crosshair_dynamic_splitdist", 0, 16);

        assertDoubleInRange(v, "cl_crosshairsize", 1, 4);
        assertDoubleInRange(v, "cl_crosshairthickness", 0.5, 2.5);
        assertDoubleInRange(v, "cl_crosshair_outlinethickness", 0, 3);
        assertDoubleInRange(v, "cl_crosshair_dynamic_maxdist_splitratio", 0, 1);
        assertDoubleInRange(v, "cl_crosshair_dynamic_splitalpha_innermod", 0, 1);
        assertDoubleInRange(v, "cl_crosshair_dynamic_splitalpha_outermod", 0, 1);
    }

    private static void assertBool(Map<String, String> v, String key) {
        String value = v.get(key);
        assertTrue("0".equals(value) || "1".equals(value), key + " should be 0/1 but was " + value);
    }

    private static void assertIntInRange(Map<String, String> v, String key, int min, int max) {
        int value = Integer.parseInt(v.get(key));
        assertTrue(value >= min && value <= max, key + "=" + value + " out of [" + min + "," + max + "]");
    }

    private static void assertDoubleInRange(Map<String, String> v, String key, double min, double max) {
        double value = Double.parseDouble(v.get(key));
        assertTrue(value >= min && value <= max, key + "=" + value + " out of [" + min + "," + max + "]");
    }
}
