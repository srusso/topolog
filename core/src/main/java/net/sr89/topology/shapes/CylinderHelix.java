package net.sr89.topology.shapes;

import com.badlogic.gdx.math.Vector2;
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

        final int cylinderCount = cylinders.size();

        float prevX = 0, prevY = 0, prevZ = 0;

        for (MyCylinder cylinder: cylinders) {
            float indexFloat = (float) cylinder.index();
            float y = (indexFloat / cylinderCount) * 3F;
            // TODO this "works" but it makes no sense. Please refactor "y + rads".
            float x = helixX(y + rads);
            float z = helixZ(y + rads);

            final float horizontalRotation = (float) (Math.atan(x / z) + Math.PI / 2);

            final float slant = calculateSlant(new Vector3(prevX, prevY, prevZ), new Vector3(x, y, z));

            cylinder.cylinder().transform
                .setToTranslation(x, y + helixShift, z)
                // Cylinders are by default "standing up", so we need to rotate them appropriately.
                // Note that as we rotate the cylinder, its X/Y/Z axes are rotating with it.
                // Initially, the axes correspond to the green(y)/blue(z)/red(x) axes drawn by GridShape.java
                .rotate(Vector3.X, -90f)
                .rotateRad(Vector3.Z, horizontalRotation)
                .rotateRad(Vector3.X, z >= 0 ? slant : -slant) // flipping signs is a bit messed up, I'm not sure why it's needed
                .scale(0.05f, 0.1f, 0.05f);

            prevX = x;
            prevY = y;
            prevZ = z;
        }
    }

    // TODO only make this calculation once!
    private float calculateSlant(Vector3 previousCylinderPosition, Vector3 currentCylinderPosition) {
        var a = new Vector2(previousCylinderPosition.x, previousCylinderPosition.z);
        var b = new Vector2(currentCylinderPosition.x, currentCylinderPosition.z);
        var deltaH = a.dst(b);
        var deltaY = currentCylinderPosition.y - previousCylinderPosition.y;
        var angle = Math.atan(deltaY / deltaH);
        return (float) angle;
    }

    private static float helixZ(float s) {
        return (float) Math.sin(2 * Math.PI * s);
    }

    private static float helixX(float s) {
        return (float) Math.cos(2 * Math.PI * s);
    }
}
