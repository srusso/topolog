package net.sr89.topology.shapes;

import com.badlogic.gdx.math.Vector3;

import java.util.List;
import java.util.function.Consumer;

public class CylinderHelix {
    private final List<MyCylinder> cylinders;
    private float rads = 0;

    public CylinderHelix(List<MyCylinder> cylinders) {
        this.cylinders = cylinders;
    }

    public void forEach(Consumer<MyCylinder> action) {
        cylinders.forEach(action);
    }

    public void reposition(float deltaTime) {
        final float helixShift = 0.7f; // how far up we are moving the covering space above the circle

        if (rads == 0) {
            rads = (float) (Math.PI / 3);
        } else {
            rads = rads + deltaTime * 0.02f;
        }

        for (MyCylinder cylinder: cylinders) {
            float newX = helixX(cylinder.getS() + rads);
            float newY = cylinder.getNewY();
            float newZ = helixZ(cylinder.getS() + rads);

            float horizontalRotation = (float) (Math.atan(newX / newZ) + Math.PI / 2);

            cylinder.getCylinder().transform
                .setToTranslation(newX, newY + helixShift, newZ)
                // Cylinders are by default "standing up", so we need to rotate them appropriately.
                // Note that as we rotate the cylinder, its X/Y/Z axes are rotating with it.
                // Initially, the axes correspond to the green(y)/blue(z)/red(x) axes drawn by GridShape.java
                .rotate(Vector3.X, -90f)
                .rotateRad(Vector3.Z, horizontalRotation)
                .rotate(Vector3.X, 30f)
                .scale(0.05f, 0.2f, 0.05f);
        }
    }

    private static float helixZ(float s) {
        return (float) Math.sin(2 * Math.PI * s);
    }

    private static float helixX(float s) {
        return (float) Math.cos(2 * Math.PI * s);
    }
}
