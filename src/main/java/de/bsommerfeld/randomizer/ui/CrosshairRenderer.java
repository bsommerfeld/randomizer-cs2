package de.bsommerfeld.randomizer.ui;

import de.bsommerfeld.randomizer.config.CrosshairSettings;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Draws a crosshair from parsed {@link CrosshairSettings} onto a canvas. This is a visual
 * approximation of CS2's classic (static) crosshair — the scale factors below map CS2's convar units
 * to pixels closely enough for a preview, not pixel-perfectly. Dynamic styles are drawn static.
 */
final class CrosshairRenderer {

    private static final double LENGTH_PER_UNIT = 6;   // cl_crosshairsize -> line length
    private static final double THICKNESS_PER_UNIT = 2; // cl_crosshairthickness -> line width
    private static final double BASE_GAP = 4;           // gap at cl_crosshairgap 0
    private static final double GAP_PER_UNIT = 1.5;     // cl_crosshairgap -> extra gap
    private static final double OUTLINE_PER_UNIT = 2;   // cl_crosshair_outlinethickness -> outline px

    private CrosshairRenderer() {
    }

    /** Draws the crosshair centered at ({@code cx}, {@code cy}). */
    static void draw(GraphicsContext g, double cx, double cy, CrosshairSettings s) {
        double length = Math.max(0, s.size * LENGTH_PER_UNIT);
        double thickness = Math.max(1, s.thickness * THICKNESS_PER_UNIT);
        double gap = Math.max(0, BASE_GAP + s.gap * GAP_PER_UNIT);
        double outline = s.drawOutline && s.outlineThickness > 0
                ? Math.max(1, s.outlineThickness * OUTLINE_PER_UNIT)
                : 0;
        Color color = color(s);

        // Left / right arms
        bar(g, cx - gap - length, cy - thickness / 2, length, thickness, color, outline);
        bar(g, cx + gap, cy - thickness / 2, length, thickness, color, outline);
        // Bottom arm, and top arm unless it is a T crosshair
        bar(g, cx - thickness / 2, cy + gap, thickness, length, color, outline);
        if (!s.tStyle) {
            bar(g, cx - thickness / 2, cy - gap - length, thickness, length, color, outline);
        }
        // Center dot
        if (s.dot) {
            bar(g, cx - thickness / 2, cy - thickness / 2, thickness, thickness, color, outline);
        }
    }

    private static void bar(GraphicsContext g, double x, double y, double w, double h, Color color, double outline) {
        if (w <= 0 || h <= 0) {
            return;
        }
        if (outline > 0) {
            g.setFill(Color.rgb(0, 0, 0, color.getOpacity()));
            g.fillRect(x - outline, y - outline, w + 2 * outline, h + 2 * outline);
        }
        g.setFill(color);
        g.fillRect(x, y, w, h);
    }

    /** Index 5 uses the custom RGB convars; the presets (0–4) are approximate. */
    private static Color color(CrosshairSettings s) {
        double alpha = s.useAlpha ? clamp(s.alpha) / 255.0 : 1.0;
        if (s.colorIndex == 5) {
            return Color.rgb(clamp(s.red), clamp(s.green), clamp(s.blue), alpha);
        }
        return switch (s.colorIndex) {
            case 0 -> Color.rgb(255, 0, 0, alpha);     // red
            case 1 -> Color.rgb(0, 255, 0, alpha);     // green
            case 2 -> Color.rgb(255, 255, 0, alpha);   // yellow
            case 3 -> Color.rgb(0, 0, 255, alpha);     // blue
            case 4 -> Color.rgb(0, 255, 255, alpha);   // cyan
            default -> Color.rgb(0, 255, 0, alpha);
        };
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }
}
