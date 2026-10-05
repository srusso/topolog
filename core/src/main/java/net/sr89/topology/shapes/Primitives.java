package net.sr89.topology.shapes;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;

/** Small shapes that the worlds use in many places: balls, and rods that can be put between two points. */
public final class Primitives {
    private static final Quaternion ROTATION = new Quaternion();
    private static final Vector3 DIRECTION = new Vector3(), MIDDLE = new Vector3(), SCALE = new Vector3();

    private Primitives() {}

    /** A ball with the given diameter, centered at the origin. */
    public static Model ball(float diameter, Color color) {
        return new ModelBuilder().createSphere(diameter, diameter, diameter, 16, 16,
            new Material(ColorAttribute.createDiffuse(color)), Usage.Position | Usage.Normal);
    }

    /** A rod of length 1 and diameter 1, along the Y axis, centered at the origin. Use {@link #place} to put it somewhere. */
    public static Model rod(Color color) {
        return new ModelBuilder().createCylinder(1f, 1f, 1f, 8,
            new Material(ColorAttribute.createDiffuse(color)), Usage.Position | Usage.Normal);
    }

    /** Puts an instance of a {@link #rod} between two points. */
    public static void place(ModelInstance rod, Vector3 from, Vector3 to, float thickness) {
        DIRECTION.set(to).sub(from);
        final float length = DIRECTION.len();
        MIDDLE.set(from).add(to).scl(0.5f);
        if (length < 1e-6f) {
            rod.transform.setToScaling(0f, 0f, 0f);
            return;
        }
        ROTATION.setFromCross(Vector3.Y, DIRECTION.nor());
        rod.transform.set(MIDDLE, ROTATION, SCALE.set(thickness, length, thickness));
    }

    /** A color of the rainbow, for the given fraction (0 to 1) of the way around the color wheel. */
    public static Color hue(float fraction, float saturation, float value) {
        return new Color().fromHsv(360f * (fraction - (float) Math.floor(fraction)), saturation, value);
    }
}
