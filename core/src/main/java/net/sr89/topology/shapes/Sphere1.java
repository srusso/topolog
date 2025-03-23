package net.sr89.topology.shapes;

import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.math.Vector3;
import net.sr89.topology.spaces.Sphere;

import java.util.List;

/**
 * Physical representation of S1, the unit sphere in 2 dimensions, or its covering space.
 */
public abstract class Sphere1 implements Sphere {
    private final List<MyCylinder> cylinders;
    private float rads = 0;

    public Sphere1(List<MyCylinder> cylinders) {
        this.cylinders = cylinders;
    }

    protected abstract float helixX(int count, float indexFloat, float s);

    protected abstract float helixY(int count, float indexFloat);

    protected abstract float helixZ(int count, float indexFloat, float s);

    protected abstract float calculateSlant(int cylinderCount, float rads);

    /**
     * How far up we are moving the covering space above the circle.
      */
    protected abstract float upwardTranslation();

    protected abstract CylinderPosition calculatePosition(int count, int index, float rads);

    public void render(ModelBatch modelBatch, Environment environment) {
        cylinders.forEach(o -> modelBatch.render(o.cylinder(), environment));
    }

    public void reposition(float deltaTime) {
         final float helixShift = upwardTranslation();

        if (rads == 0) {
            rads = (float) (Math.PI / 3);
        } else {
            rads = rads + deltaTime * 0.1f;
        }

        final int cylinderCount = cylinders.size();
        final float slant = calculateSlant(cylinderCount, rads);

        for (int i = 0 ; i < cylinderCount ; i++) {
            MyCylinder cylinder = cylinders.get(i);
            CylinderPosition position = calculatePosition(cylinderCount, i, rads);

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

    protected record CylinderPosition (float x, float y, float z, float horizontalRotation) {}
}
