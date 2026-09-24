package de.bsommerfeld.randomizer.gsi.event;

import com.cs2gsi.events.CS2GameEvent;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

/** Reflective access to the public instance fields an event exposes. */
final class EventFields {

    private EventFields() {
    }

    /** Every public, non-static field of the event (including inherited ones). */
    static List<Field> of(CS2GameEvent event) {
        return Arrays.stream(event.getClass().getFields())
                .filter(field -> !Modifier.isStatic(field.getModifiers()))
                .toList();
    }

    /** Reads {@code field} from {@code event}, or a placeholder when it is inaccessible. */
    static Object read(Field field, CS2GameEvent event) {
        try {
            return field.get(event);
        } catch (ReflectiveOperationException e) {
            return "<unreadable>";
        }
    }
}
