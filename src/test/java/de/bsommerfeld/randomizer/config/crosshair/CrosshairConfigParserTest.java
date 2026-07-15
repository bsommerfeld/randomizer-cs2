package de.bsommerfeld.randomizer.config.crosshair;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrosshairConfigParserTest {

    @TempDir
    Path tempDir;

    private final CrosshairConfigParser parser = new CrosshairConfigParser();

    private Crosshair parse(String vdf) throws Exception {
        Path file = tempDir.resolve("cs2_user_convars.vcfg");
        Files.writeString(file, vdf);
        return parser.parse(file);
    }

    @Test
    void collectsCrosshairConvarsFromNestedVdfAndIgnoresOthers() throws Exception {
        Crosshair crosshair = parse("""
                "UserConvars"
                {
                    "convars"
                    {
                        "cl_crosshairstyle" "4"
                        "cl_crosshairsize" "0.5"
                        "cl_crosshaircolor" "5"
                        "cl_fixedcrosshairgap" "3"
                        "sensitivity" "1.5"
                    }
                }
                """);

        assertEquals(4, crosshair.convars().size(), "every crosshair-related convar should be collected");
        assertTrue(crosshair.convars().containsKey("cl_fixedcrosshairgap"),
                "differently-prefixed crosshair convars too");
        assertFalse(crosshair.convars().containsKey("sensitivity"), "unrelated convars must be ignored");
        assertEquals("0.5", crosshair.convars().get("cl_crosshairsize"));
    }

    @Test
    void parsesSettingsWithCorrectTypesAndCustomColor() throws Exception {
        Crosshair crosshair = parse("""
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

        CrosshairSettings settings = crosshair.settings();

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
}
