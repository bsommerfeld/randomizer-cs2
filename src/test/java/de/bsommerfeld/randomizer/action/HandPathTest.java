package de.bsommerfeld.randomizer.action;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HandPathTest {

    @Test
    void startsAndEndsSlowAndIsFastestAtHalfTime() {
        double firstTenth = HandPath.at(0.1, 1000, 0, 0).x();
        double middleTenth = HandPath.at(0.55, 1000, 0, 0).x() - HandPath.at(0.45, 1000, 0, 0).x();
        double lastTenth = 1000 - HandPath.at(0.9, 1000, 0, 0).x();

        assertTrue(firstTenth < 10, "first tenth " + firstTenth);
        assertTrue(middleTenth > 180, "middle tenth " + middleTenth);
        assertEquals(firstTenth, lastTenth, 1e-9);
        assertEquals(500, HandPath.at(0.5, 1000, 0, 0).x(), 1e-9);
    }

    @Test
    void bowsSidewaysOnTheWayAndStartsAndEndsOnTheLine() {
        assertEquals(new HandPath.Point(500, 100), HandPath.at(0.5, 1000, 0, 0.1));
        assertEquals(new HandPath.Point(-100, 500), HandPath.at(0.5, 0, 1000, 0.1), "right of a move downwards is left");
        assertEquals(new HandPath.Point(0, 0), HandPath.at(0, 1000, 0, 0.1));
        assertEquals(1000, HandPath.at(1, 1000, 0, 0.1).x(), 1e-9);
        assertEquals(0, HandPath.at(1, 1000, 0, 0.1).y(), 1e-9);
    }
}
