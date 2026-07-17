package de.bsommerfeld.randomizer.config.crosshair;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrosshairConvarTest {

    @Test
    void catalogueIsExactlyTheTwentyOneConvarsInOrder() {
        List<String> keys = java.util.Arrays.stream(CrosshairConvar.values())
                .map(CrosshairConvar::key)
                .toList();

        assertEquals(List.of(
                "cl_crosshair_drawoutline",
                "cl_crosshair_dynamic_maxdist_splitratio",
                "cl_crosshair_dynamic_splitalpha_innermod",
                "cl_crosshair_dynamic_splitalpha_outermod",
                "cl_crosshair_dynamic_splitdist",
                "cl_crosshair_outlinethickness",
                "cl_crosshair_t",
                "cl_crosshairalpha",
                "cl_crosshaircolor",
                "cl_crosshaircolor_b",
                "cl_crosshaircolor_g",
                "cl_crosshaircolor_r",
                "cl_crosshairdot",
                "cl_crosshairgap",
                "cl_crosshairgap_useweaponvalue",
                "cl_crosshairsize",
                "cl_crosshairstyle",
                "cl_crosshairthickness",
                "cl_crosshairusealpha",
                "cl_fixedcrosshairgap",
                "cl_crosshair_recoil"), keys);
    }

    @Test
    void canonicalValuesKeepsKnownFillsMissingAndDropsOthers() {
        Map<String, String> source = new LinkedHashMap<>();
        source.put("cl_crosshairsize", "3");            // known → kept
        source.put("sensitivity", "1.5");               // unrelated → dropped
        source.put("cl_crosshair_unknown", "42");        // crosshair-ish but not canonical → dropped

        Map<String, String> canonical = CrosshairConvar.canonicalValues(source);

        assertEquals(CrosshairConvar.values().length, canonical.size(), "exactly the canonical keys");
        assertEquals("3", canonical.get("cl_crosshairsize"), "known value kept");
        assertEquals("0", canonical.get("cl_crosshairdot"), "missing key filled with its default");
        assertFalse(canonical.containsKey("sensitivity"), "unrelated key dropped");
        assertFalse(canonical.containsKey("cl_crosshair_unknown"), "non-canonical crosshair key dropped");
    }

    @Test
    void defaultsCoverEveryConvar() {
        Map<String, String> defaults = CrosshairConvar.defaults();

        assertEquals(CrosshairConvar.values().length, defaults.size());
        for (CrosshairConvar convar : CrosshairConvar.values()) {
            assertTrue(defaults.containsKey(convar.key()), "default for " + convar.key());
        }
    }
}
