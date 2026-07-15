package de.bsommerfeld.randomizer.config.crosshair;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrosshairSettingsTest {

    @Test
    void parsesNumericBooleanFlags() {
        CrosshairSettings settings = CrosshairSettings.fromConvars(Map.of(
                "cl_crosshairusealpha", "1",
                "cl_crosshairdot", "0",
                "cl_crosshair_drawoutline", "0",
                "cl_crosshairalpha", "200",
                "cl_fixedcrosshairgap", "3"));

        assertTrue(settings.useAlpha, "'1' should parse as true");
        assertFalse(settings.dot, "'0' should parse as false");
        assertFalse(settings.drawOutline);
        assertEquals(200, settings.alpha);
        assertEquals(3.0, settings.fixedGap, 1e-6);
    }

    @Test
    void missingConvarsFallBackToDefaults() {
        CrosshairSettings settings = CrosshairSettings.fromConvars(Map.of());

        assertEquals(4, settings.style, "default crosshair style");
        assertEquals(1, settings.colorIndex, "default color index");
        assertTrue(settings.useAlpha, "alpha is used by default");
    }

    @Test
    void unparseableValuesFallBackToDefaults() {
        CrosshairSettings settings = CrosshairSettings.fromConvars(Map.of(
                "cl_crosshairsize", "not-a-number",
                "cl_crosshairdot", "maybe"));

        assertEquals(5.0, settings.size, 1e-6, "default size");
        assertFalse(settings.dot, "default dot");
    }
}
