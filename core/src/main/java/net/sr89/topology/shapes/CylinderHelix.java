package net.sr89.topology.shapes;

import com.badlogic.gdx.math.Vector3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class CylinderHelix {
    private final List<MyCylinder> cylinders;
    private final Map<String, Float> radians;

    public CylinderHelix(List<MyCylinder> cylinders) {
        this.cylinders = cylinders;
        this.radians = new HashMap<>();
    }

    public void forEach(Consumer<MyCylinder> action) {
        cylinders.forEach(action);
    }

    public void reposition(float deltaTime) {
        final float helixShift = 0.7f; // how far up we are moving the covering space above the circle

        for (MyCylinder cylinder: cylinders) {
            float newX = cylinder.getNewX();
            float newY = cylinder.getNewY();
            float newZ = cylinder.getNewZ();

            float horizontalRotation = (float) (Math.atan(newX / newZ) + Math.PI / 2);

            cylinder.getCylinder().transform
                .setToTranslation(newX, newY + helixShift, newZ)
                .rotate(Vector3.X, -90f)
                .rotateRad(Vector3.Z, horizontalRotation)
                .scale(0.05f, 0.2f, 0.05f);
        }
    }
}
