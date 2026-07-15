package de.bsommerfeld.randomizer.gsi.event;

/**
 * Renders a single event-field value into display text. Each renderer handles exactly one value
 * shape; new shapes are supported by registering another renderer, never by editing an existing
 * one (open/closed principle).
 */
public interface ValueRenderer {

    /** Whether this renderer knows how to render {@code value}. */
    boolean supports(Object value);

    /** Renders {@code value}; only invoked when {@link #supports(Object)} returned {@code true}. */
    String render(Object value);
}
