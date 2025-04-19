package net.sr89.topology.shapes;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

import java.util.List;

/**
 * Covering space for the unit sphere, see {@link UnitSphere}.
 */
public class CylinderHelix extends Sphere1 {
    public CylinderHelix(List<MyCylinder> cylinders) {
        super(cylinders);
    }

    @Override
    protected float helixX(int count, float index, float rads) {
        // for the helix we want to make more than "one circle" while going up,
        // so we decide the x and z coordinates based on the y coordinate and how many
        // radians we want to rotate from the "base" position
        return (float) Math.cos(2 * Math.PI * (helixY(count, index) + rads));
    }

    @Override
    protected float helixY(int count, float index) {
        return (index / count) * 3F;
    }

    @Override
    protected float helixZ(int count, float index, float rads) {
        return (float) Math.sin(2 * Math.PI * (helixY(count, index) + rads));
    }

    @Override
    protected float calculateSlant(int cylinderCount, float rads) {
        final float firstY = helixY(cylinderCount, 0);
        final float firstX = helixX(cylinderCount, 0, firstY + rads), firstZ = helixZ(cylinderCount, 0, firstY + rads);

        final float secondY = helixY(cylinderCount, 1);
        final float secondX = helixX(cylinderCount, 1, secondY + rads), secondZ = helixZ(cylinderCount, 1, secondY + rads);

        // the positions of the centers of the first and second cylinders
        final Vector3 firstCylinderPosition = new Vector3(firstX, firstY, firstZ);
        final Vector3 secondCylinderPosition = new Vector3(secondX, secondY, secondZ);

        var a = new Vector2(firstCylinderPosition.x, firstCylinderPosition.z);
        var b = new Vector2(secondCylinderPosition.x, secondCylinderPosition.z);
        var deltaH = a.dst(b);
        var deltaY = secondCylinderPosition.y - firstCylinderPosition.y;
        var angle = Math.atan(deltaY / deltaH);
        return (float) angle;
    }

    @Override
    protected float upwardTranslation() {
        return 0.7F;
    }
}
