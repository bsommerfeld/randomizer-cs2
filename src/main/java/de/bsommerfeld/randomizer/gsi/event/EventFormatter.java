package de.bsommerfeld.randomizer.gsi.event;

import com.cs2gsi.events.CS2GameEvent;
import de.bsommerfeld.randomizer.gsi.GsiEvent;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Turns a {@link CS2GameEvent} into a display-ready {@link GsiEvent}: a one-line summary plus
 * multi-line details. How a single field value reads is {@link EventValues}' business.
 */
public final class EventFormatter {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final int SUMMARY_MAX_LENGTH = 160;

    private final Clock clock;

    public EventFormatter() {
        this(Clock.systemDefaultZone());
    }

    /** @param clock stamps each event; a fixed one makes the output reproducible */
    public EventFormatter(Clock clock) {
        this.clock = clock;
    }

    public GsiEvent format(CS2GameEvent event) {
        String time = LocalTime.now(clock).format(TIME);
        String name = event.getClass().getSimpleName();
        return new GsiEvent(summary(time, name, event), details(time, name, event));
    }

    private String summary(String time, String name, CS2GameEvent event) {
        return time + "  " + abbreviate(name + summarizeFields(event));
    }

    private String details(String time, String name, CS2GameEvent event) {
        return "Time:  " + time + System.lineSeparator()
                + "Event: " + name + System.lineSeparator()
                + System.lineSeparator()
                + formatFields(event);
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
            // Node values render as JSON objects or bracket trees, too long for the summary line
            if (!text.startsWith("{") && !text.startsWith("[")) {
                sb.append(' ').append(field.getName()).append('=').append(text);
            }
        }
        return sb.toString();
    }

    /** Lists every public event field with its rendered value. */
    private static String formatFields(CS2GameEvent event) {
        List<Field> fields = EventFields.of(event);
        if (fields.isEmpty()) {
            return "(no further data)";
        }
        StringBuilder sb = new StringBuilder();
        for (Field field : fields) {
            Object value = EventFields.read(field, event);
            sb.append(field.getName()).append(':').append(System.lineSeparator())
                    .append(EventValues.render(value).indent(2))
                    .append(System.lineSeparator());
        }
        return sb.toString().stripTrailing();
    }
}
