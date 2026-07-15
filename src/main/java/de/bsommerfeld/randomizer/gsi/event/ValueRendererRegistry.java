package de.bsommerfeld.randomizer.gsi.event;

import java.util.ArrayList;
import java.util.List;

/**
 * Ordered list of {@link ValueRenderer}s with a fixed fallback. The first registered renderer that
 * {@link ValueRenderer#supports supports} a value renders it; if none does, the fallback handles it.
 * Registration order is precedence order, so more specific renderers must be registered first.
 *
 * <p>Rendering is extended by registering another renderer - existing renderers stay untouched
 * (open/closed principle).
 */
public final class ValueRendererRegistry {

    private final List<ValueRenderer> renderers = new ArrayList<>();
    private final ValueRenderer fallback = new DefaultValueRenderer();

    /** A registry pre-populated with the built-in renderers. */
    public static ValueRendererRegistry withDefaults() {
        return new ValueRendererRegistry().register(new PlayerValueRenderer());
    }

    /** Appends a renderer; it takes precedence over the fallback but yields to earlier renderers. */
    public ValueRendererRegistry register(ValueRenderer renderer) {
        renderers.add(renderer);
        return this;
    }

    /** Renders {@code value} with the first supporting renderer, or the fallback. */
    public String render(Object value) {
        for (ValueRenderer renderer : renderers) {
            if (renderer.supports(value)) {
                return renderer.render(value);
            }
        }
        return fallback.render(value);
    }
}
