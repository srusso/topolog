package net.sr89.topology.shapes;

import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import net.sr89.topology.spaces.Sphere;

import java.util.List;

/**
 * Physical representation of S1, the unit sphere in 2 dimensions, or its covering space.
 */
public class Sphere1 implements Sphere {
    private final List<MyCylinder> cylinders;
    private float rads = 0;

    public Sphere1(List<MyCylinder> cylinders) {
        this.cylinders = cylinders;
    }

    public void render(ModelBatch modelBatch, Environment environment) {
        cylinders.forEach(o -> modelBatch.render(o.cylinder(), environment));
    }

    public void reposition(float deltaTime) {
        final float helixShift = 0.7f; // how far up we are moving the covering space above the circle

        if (rads == 0) {
            rads = (float) (Math.PI / 3);
        } else {
            rads = rads + deltaTime * 0.02f;
        }

        final int cylinderCount = cylinders.size();
        final float slant = calculateSlant(cylinderCount);

        for (int i = 0 ; i < cylinderCount ; i++) {
            MyCylinder cylinder = cylinders.get(i);
            CylinderPosition position = calculatePosition(cylinderCount, i);

            cylinder.cylinder().transform
                // start by centering the cylinder at its position
                .setToTranslation(position.x, position.y + helixShift, position.z)
                // Cylinders are by default "standing up", so we need to rotate them appropriately.
                // Note that as we rotate the cylinder, its X/Y/Z axes are rotating with it.
                // Initially, the axes correspond to the green(y)/blue(z)/red(x) axes drawn by GridShape.java
                .rotate(Vector3.X, -90f)
                .rotateRad(Vector3.Z, position.horizontalRotation)
                .rotateRad(Vector3.X, position.z >= 0 ? slant : -slant) // flipping signs is a bit messed up, I'm not sure why it's needed
                .scale(0.05f, 0.1f, 0.05f);
        }
    }

    private CylinderPosition calculatePosition(int count, int index) {
        float indexFloat = (float) index;
        float y = helixY(count, indexFloat);
        // TODO this "works" but it makes no sense. Please refactor "y + rads".
        float x = helixX(y + rads);
        float z = helixZ(y + rads);

        final float horizontalRotation = (float) (Math.atan(x / z) + Math.PI / 2);

        return new CylinderPosition(x, y, z, horizontalRotation);
    }

    private float calculateSlant(int cylinderCount) {
        final float firstY = helixY(cylinderCount, 0);
        final float firstX = helixX(firstY + rads), firstZ = helixZ(firstY + rads);

        final float secondY = helixY(cylinderCount, 1);
        final float secondX = helixX(secondY + rads), secondZ = helixZ(secondY + rads);

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

    private static float helixX(float s) {
        return (float) Math.cos(2 * Math.PI * s);
    }

    private static float helixY(int count, float indexFloat) {
        return (indexFloat / count) * 3F;
    }

    private static float helixZ(float s) {
        return (float) Math.sin(2 * Math.PI * s);
    }

    record CylinderPosition (float x, float y, float z, float horizontalRotation) {}
}
