package net.sr89.topology.shapes;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import net.sr89.topology.HexColors;
import net.sr89.topology.math.Curve;

/** Factories for the tubes used by the worlds. */
public final class Tubes {
    private static final int SIDES = 12;
    private static final float TUBE_RADIUS = 0.025f;

    private static final int HELIX_SAMPLES = 400;
    private static final float HELIX_TURNS = 3f; // also the height of the helix
    private static final float HELIX_UPWARD_TRANSLATION = 0.7f;

    private static final int CIRCLE_SAMPLES = 200;

    private Tubes() {}

    /** S1: all points at distance 1 from the origin in the XZ plane. It never moves. */
    public static Tube unitCircle() {
        final Curve circle = (t, out) -> out.set((float) Math.cos(2 * Math.PI * t), 0f, (float) Math.sin(2 * Math.PI * t));
        return new Tube(TubeMesh.build(circle, true, CIRCLE_SAMPLES, SIDES, TUBE_RADIUS, HexColors.greenPastel()), null);
    }

    /** Covering space for S1, see {@link #unitCircle()}. It slowly rotates around the Y axis. */
    public static Tube helix() {
        final Curve helix = (t, out) -> {
            final double angle = 2 * Math.PI * HELIX_TURNS * t;
            out.set((float) Math.cos(angle), HELIX_TURNS * t, (float) Math.sin(angle));
        };
        return new Tube(
            TubeMesh.build(helix, false, HELIX_SAMPLES, SIDES, TUBE_RADIUS, HexColors.greenPastel()),
            // Turning the helix is just rotating the whole mesh about Y.
            (transform, turns) -> transform
                .setToTranslation(0f, HELIX_UPWARD_TRANSLATION, 0f)
                .rotateRad(Vector3.Y, -MathUtils.PI2 * turns));
    }
}
