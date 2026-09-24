package de.bsommerfeld.randomizer.gsi.event;

import com.cs2gsi.events.CS2GameEvent;
import de.bsommerfeld.randomizer.gsi.GsiEvent;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertTrue;

class EventFormatterTest {

    private static final Clock NOON = Clock.fixed(Instant.parse("2026-01-01T12:34:56Z"), ZoneOffset.UTC);

    /** Stands in for a library event: the formatter only looks at the class name and public fields. */
    public static final class ScoreChanged extends CS2GameEvent {
        public final String team = "CT";
        public final int newValue = 7;
    }

    @Test
    void stampsTheEventWithTheInjectedClockAndListsItsFields() {
        GsiEvent event = new EventFormatter(NOON).format(new ScoreChanged());

        assertTrue(event.summary().startsWith("12:34:56  ScoreChanged"), event.summary());
        assertTrue(event.summary().contains(" team=CT"), event.summary());
        assertTrue(event.summary().contains(" newValue=7"), event.summary());
        assertTrue(event.details().contains("Time:  12:34:56"), event.details());
        assertTrue(event.details().contains("Event: ScoreChanged"), event.details());
    }
}
