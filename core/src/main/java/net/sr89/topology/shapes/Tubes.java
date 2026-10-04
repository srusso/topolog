package net.sr89.topology.shapes;

import net.sr89.topology.HexColors;
import net.sr89.topology.math.Curve;

/** Factories for the tubes used by the worlds. */
public final class Tubes {
    private static final int SIDES = 12;
    private static final float TUBE_RADIUS = 0.025f;

    private static final int HELIX_SAMPLES = 400;
    /** How many times the helix goes around. This is also its height, and how many points of it lie above each point of the circle. */
    public static final int HELIX_SHEETS = 3;
    private static final float HELIX_UPWARD_TRANSLATION = 0.7f;

    private static final int CIRCLE_SAMPLES = 200;

    private Tubes() {}

    /** S1: all points at distance 1 from the origin in the XZ plane. */
    public static Tube unitCircle() {
        final Curve circle = (t, out) -> out.set((float) Math.cos(2 * Math.PI * t), 0f, (float) Math.sin(2 * Math.PI * t));
        return new Tube(TubeMesh.build(circle, true, CIRCLE_SAMPLES, SIDES, TUBE_RADIUS, HexColors.greenPastel()));
    }

    /**
     * Covering space for S1, see {@link #unitCircle()}: it goes around {@link #HELIX_SHEETS} times while going up,
     * starting {@link #HELIX_UPWARD_TRANSLATION} above the circle.
     */
    public static Tube helix() {
        final Curve helix = (t, out) -> {
            final double angle = 2 * Math.PI * HELIX_SHEETS * t;
            out.set((float) Math.cos(angle), HELIX_SHEETS * t + HELIX_UPWARD_TRANSLATION, (float) Math.sin(angle));
        };
        return new Tube(TubeMesh.build(helix, false, HELIX_SAMPLES, SIDES, TUBE_RADIUS, HexColors.bluePastel()));
    }

    /**
     * The covering map from the helix to the circle just drops the helix point straight down onto the circle.
     * Inverting it: the helix points above the circle point at a given angle are at the heights returned here,
     * one for each of the {@link #HELIX_SHEETS} sheets of the helix.
     *
     * @param circleAngle the angle of the circle point around the Y axis, in full turns, from 0 to 1
     * @param sheet 0 to {@link #HELIX_SHEETS} - 1
     * @return the height (world Y coordinate) of the helix point above the circle point
     */
    public static float helixHeightOver(float circleAngle, int sheet) {
        return circleAngle + sheet + HELIX_UPWARD_TRANSLATION;
    }
}
