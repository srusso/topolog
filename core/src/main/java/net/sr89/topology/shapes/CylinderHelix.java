package net.sr89.topology.shapes;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import net.sr89.topology.HexColors;

/**
 * Covering space for the unit sphere, see {@link UnitCircle}.
 */
public class CylinderHelix extends Sphere1 {
    private static final int SAMPLES = 400;
    private static final int SIDES = 12;
    private static final float TUBE_RADIUS = 0.025f;
    private static final float TURNS = 3f; // also the height of the helix
    private static final float UPWARD_TRANSLATION = 0.7f;

    public CylinderHelix() {
        super(TubeMesh.build(CylinderHelix::pointAt, false, SAMPLES, SIDES, TUBE_RADIUS, HexColors.GREEN_PASTEL));
    }

    private static void pointAt(float t, Vector3 out) {
        final float turns = TURNS * t;
        out.set(MathUtils.cos(MathUtils.PI2 * turns), turns, MathUtils.sin(MathUtils.PI2 * turns));
    }

    @Override
    protected void updateTransform(Matrix4 transform, float rads) {
        // Turning the helix is just rotating the whole mesh about Y.
        transform.setToTranslation(0f, UPWARD_TRANSLATION, 0f).rotateRad(Vector3.Y, -MathUtils.PI2 * rads);
    }
}
