package de.bsommerfeld.randomizer.config;

import java.util.Map;

/**
 * The crosshair-relevant {@code cl_crosshair*} convars from {@code cs2_user_convars.vcfg}, parsed
 * into typed values with CS2 defaults for anything missing. Pure data - no UI dependency; the
 * renderer turns these into pixels.
 */
public final class CrosshairSettings {

    public final int style;
    public final double size;
    public final double thickness;
    public final double gap;
    public final double fixedGap;
    public final boolean dot;
    public final boolean tStyle;
    public final boolean drawOutline;
    public final double outlineThickness;
    public final int colorIndex;
    public final int red;
    public final int green;
    public final int blue;
    public final int alpha;
    public final boolean useAlpha;

    public CrosshairSettings(int style, double size, double thickness, double gap, double fixedGap,
                             boolean dot, boolean tStyle, boolean drawOutline, double outlineThickness,
                             int colorIndex, int red, int green, int blue, int alpha, boolean useAlpha) {
        this.style = style;
        this.size = size;
        this.thickness = thickness;
        this.gap = gap;
        this.fixedGap = fixedGap;
        this.dot = dot;
        this.tStyle = tStyle;
        this.drawOutline = drawOutline;
        this.outlineThickness = outlineThickness;
        this.colorIndex = colorIndex;
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.alpha = alpha;
        this.useAlpha = useAlpha;
    }

    /** Builds the settings from a {@code convar -> value} map (missing keys fall back to CS2 defaults). */
    public static CrosshairSettings fromConvars(Map<String, String> convars) {
        return new CrosshairSettings(
                intOf(convars, "cl_crosshairstyle", 4),
                doubleOf(convars, "cl_crosshairsize", 5),
                doubleOf(convars, "cl_crosshairthickness", 0.5),
                doubleOf(convars, "cl_crosshairgap", 0),
                doubleOf(convars, "cl_fixedcrosshairgap", 3),
                boolOf(convars, "cl_crosshairdot", false),
                boolOf(convars, "cl_crosshair_t", false),
                boolOf(convars, "cl_crosshair_drawoutline", false),
                doubleOf(convars, "cl_crosshair_outlinethickness", 1),
                intOf(convars, "cl_crosshaircolor", 1),
                intOf(convars, "cl_crosshaircolor_r", 255),
                intOf(convars, "cl_crosshaircolor_g", 255),
                intOf(convars, "cl_crosshaircolor_b", 255),
                intOf(convars, "cl_crosshairalpha", 255),
                boolOf(convars, "cl_crosshairusealpha", true));
    }

    private static int intOf(Map<String, String> convars, String key, int fallback) {
        double value = doubleOf(convars, key, Double.NaN);
        return Double.isNaN(value) ? fallback : (int) Math.round(value);
    }

    private static double doubleOf(Map<String, String> convars, String key, double fallback) {
        String value = convars.get(key);
        if (value == null) {
            return fallback;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static boolean boolOf(Map<String, String> convars, String key, boolean fallback) {
        String value = convars.get(key);
        if (value == null) {
            return fallback;
        }
        String trimmed = value.trim();
        if (trimmed.equalsIgnoreCase("true") || trimmed.equals("1")) {
            return true;
        }
        if (trimmed.equalsIgnoreCase("false") || trimmed.equals("0")) {
            return false;
        }
        return fallback;
    }
}
