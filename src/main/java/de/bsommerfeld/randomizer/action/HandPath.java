package de.bsommerfeld.randomizer.action;

/**
 * The way a hand moves the mouse during a turn, so the view does not glide along a ruler at constant
 * speed. The randomizer draws the random parts and adds a tremor of its own on top.
 */
final class HandPath {

    /** Mouse counts from the start of the turn. */
    record Point(double x, double y) {
    }

    /** How far a path bows to the side at most, as a share of its length. */
    static final double MAX_BEND = 0.15;

    private HandPath() {
    }

    /**
     * Where the hand has the mouse once the share {@code t} of the turn's time has passed, on the way
     * to {@code dx}, {@code dy}. The timing is minimum jerk, the model for a person reaching for a
     * target (Flash and Hogan, 1985): slow at the start, fastest at half time, slow at the end.
     * {@code bend} bows the path sideways by that share of its length at the halfway point, positive to
     * the right of the direction of travel. Start and end have no bow.
     */
    static Point at(double t, int dx, int dy, double bend) {
        double done = t * t * t * (10 - 15 * t + 6 * t * t);
        double side = bend * Math.sin(Math.PI * done);
        return new Point(dx * done - dy * side, dy * done + dx * side);
    }
}
