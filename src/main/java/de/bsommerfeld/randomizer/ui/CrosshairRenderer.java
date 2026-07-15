package de.bsommerfeld.randomizer.ui;

import de.bsommerfeld.randomizer.config.CrosshairSettings;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Draws a crosshair from parsed {@link CrosshairSettings} onto a canvas. The size/thickness/gap
 * pixel formulas are adapted from the CS2 crosshair generator
 * <a href="https://github.com/omar-anwari/CS-Crosshair-Gen">omar-anwari/CS-Crosshair-Gen</a>, so the
 * proportions match CS2 closely. At {@code scale == 1} the reference draws 1080p-native pixels, so
 * on a 1080p monitor the preview is 1:1 with the game; pass {@code resolutionHeight / 1080} to match
 * other resolutions. Dynamic styles are drawn static.
 */
final class CrosshairRenderer {

    private CrosshairRenderer() {
    }

    /** Draws the crosshair centered at ({@code cx}, {@code cy}); {@code scale} is resolution/1080. */
    static void draw(GraphicsContext g, double cx, double cy, CrosshairSettings s, double scale) {
        double length = Math.floor((s.size + 0.2222) / 0.4445) * scale;
        double thickness = Math.max(1, Math.floor((s.thickness + 0.2222) / 0.4444) * scale);
        double gap = gapUnits(s) * scale;
        double outline = s.drawOutline && s.outlineThickness > 0
                ? Math.max(1, Math.floor(s.outlineThickness * scale))
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

    /**
     * Gap in convar units (before scaling), following the reference generator: classic-static adds a
     * base of 4, the classic/dynamic styles add 5; {@code cl_crosshairgap} drives all of them.
     */
    private static double gapUnits(CrosshairSettings s) {
        double gap = s.gap;
        if (s.style == 2 || s.style == 3) { // classic / classic dynamic
            double base = gap < 0 ? -Math.floor(-gap) : Math.floor(gap);
            return base + 5;
        }
        double base = gap < -4 ? -Math.floor(-gap) : Math.floor(gap); // classic static / default
        return base + 4;
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

    /** Presets 0–4 match the reference generator's palette; index 5 uses the custom RGB convars. */
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
