package de.bsommerfeld.randomizer.gsi.event;

import com.cs2gsi.events.CS2GameEvent;
import de.bsommerfeld.randomizer.gsi.GsiEvent;

import java.lang.reflect.Field;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Turns a {@link CS2GameEvent} into a display-ready {@link GsiEvent}: a one-line summary plus
 * multi-line details. Field values are rendered through a {@link ValueRendererRegistry}, so
 * per-type rendering is added by registering a renderer rather than by changing this class
 * (open/closed principle).
 */
public final class EventFormatter {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final int SUMMARY_MAX_LENGTH = 160;

    private final ValueRendererRegistry renderers;

    public EventFormatter() {
        this(ValueRendererRegistry.withDefaults());
    }

    public EventFormatter(ValueRendererRegistry renderers) {
        this.renderers = renderers;
    }

    /** Prepares {@code event} for display. */
    public GsiEvent format(CS2GameEvent event) {
        String time = LocalTime.now().format(TIME);
        String name = event.getClass().getSimpleName();
        String summary = time + "  " + abbreviate(name + summarizeFields(event));
        String details = "Zeit:  " + time + System.lineSeparator()
                + "Event: " + name + System.lineSeparator()
                + System.lineSeparator()
                + formatFields(event);
        return new GsiEvent(summary, details);
    }

    private static String abbreviate(String value) {
        return value.length() <= SUMMARY_MAX_LENGTH
                ? value
                : value.substring(0, SUMMARY_MAX_LENGTH - 3) + "…";
    }

    /** Compact "key=value" pairs of all scalar event fields for the summary line. */
    private String summarizeFields(CS2GameEvent event) {
        StringBuilder sb = new StringBuilder();
        for (Field field : EventFields.of(event)) {
            String text = String.valueOf(EventFields.read(field, event));
            // Node values render as JSON objects/brackets - too long for the summary line
            if (!text.startsWith("{") && !text.startsWith("[")) {
                sb.append(' ').append(field.getName()).append('=').append(text);
            }
        }
        return sb.toString();
    }

    /** Lists every public event field, each value rendered via the registry. */
    private String formatFields(CS2GameEvent event) {
        List<Field> fields = EventFields.of(event);
        if (fields.isEmpty()) {
            return "(keine weiteren Daten)";
        }
        StringBuilder sb = new StringBuilder();
        for (Field field : fields) {
            Object value = EventFields.read(field, event);
            sb.append(field.getName()).append(':').append(System.lineSeparator())
                    .append(renderers.render(value).indent(2))
                    .append(System.lineSeparator());
        }
        return sb.toString().stripTrailing();
    }
}
