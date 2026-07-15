package de.bsommerfeld.randomizer.config;

import de.bsommerfeld.randomizer.vdf.VdfObject;
import de.bsommerfeld.randomizer.vdf.VdfParser;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrosshairServiceTest {

    private static Map<String, String> extract(String vdf) {
        VdfObject root = VdfParser.parse(vdf);
        Map<String, String> convars = new LinkedHashMap<>();
        CrosshairService.collectCrosshairConvars(root, convars);
        return convars;
    }

    @Test
    void collectsCrosshairConvarsFromNestedVdfAndIgnoresOthers() {
        Map<String, String> convars = extract("""
                "UserConvars"
                {
                    "convars"
                    {
                        "cl_crosshairstyle" "4"
                        "cl_crosshairsize" "0.5"
                        "cl_crosshaircolor" "5"
                        "sensitivity" "1.5"
                    }
                }
                """);

        assertEquals(3, convars.size(), "only cl_crosshair* entries should be collected");
        assertFalse(convars.containsKey("sensitivity"), "unrelated convars must be ignored");
        assertEquals("0.5", convars.get("cl_crosshairsize"));
    }

    @Test
    void parsesSettingsWithCorrectTypesAndCustomColor() {
        Map<String, String> convars = extract("""
                "convars"
                {
                    "cl_crosshairstyle" "4"
                    "cl_crosshairsize" "0.5"
                    "cl_crosshairthickness" "1"
                    "cl_crosshairgap" "-2.9"
                    "cl_crosshaircolor" "5"
                    "cl_crosshaircolor_r" "172"
                    "cl_crosshaircolor_g" "51"
                    "cl_crosshaircolor_b" "255"
                    "cl_crosshairdot" "false"
                    "cl_crosshair_t" "false"
                    "cl_crosshair_drawoutline" "true"
                    "cl_crosshair_outlinethickness" "0"
                    "cl_crosshairalpha" "255"
                    "cl_crosshairusealpha" "false"
                }
                """);

        CrosshairSettings settings = CrosshairSettings.fromConvars(convars);

        assertEquals(4, settings.style);
        assertEquals(0.5, settings.size, 1e-6);
        assertEquals(1.0, settings.thickness, 1e-6);
        assertEquals(-2.9, settings.gap, 1e-6);
        assertEquals(5, settings.colorIndex);
        assertEquals(172, settings.red);
        assertEquals(51, settings.green);
        assertEquals(255, settings.blue);
        assertEquals(255, settings.alpha);
        assertFalse(settings.dot);
        assertFalse(settings.tStyle);
        assertTrue(settings.drawOutline);
        assertEquals(0.0, settings.outlineThickness, 1e-6);
        assertFalse(settings.useAlpha);
    }

    @Test
    void missingConvarsFallBackToDefaults() {
        CrosshairSettings settings = CrosshairSettings.fromConvars(Map.of());

        assertEquals(4, settings.style, "default crosshair style");
        assertEquals(1, settings.colorIndex, "default color index");
        assertTrue(settings.useAlpha, "alpha is used by default");
    }
}
