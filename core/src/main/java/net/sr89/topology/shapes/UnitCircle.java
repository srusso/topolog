package net.sr89.topology.shapes;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.MathUtils;
import net.sr89.topology.HexColors;

/**
 * Physical representation of S1, the unit circle in 2 dimensions, all points of distance 1 from the origin.
 */
public class UnitCircle extends Sphere1 {
    private static final int SAMPLES = 200;
    private static final int SIDES = 12;
    private static final float TUBE_RADIUS = 0.025f;

    public UnitCircle() {
        super(TubeMesh.build(
            (t, out) -> out.set(MathUtils.cos(MathUtils.PI2 * t), 0f, MathUtils.sin(MathUtils.PI2 * t)),
            true, SAMPLES, SIDES, TUBE_RADIUS, HexColors.GREEN_PASTEL));
    }

    @Override
    protected void updateTransform(Matrix4 transform, float rads) {
        // the circle never moves
        transform.idt();
    }
}
