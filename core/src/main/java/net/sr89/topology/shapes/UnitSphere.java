package net.sr89.topology.shapes;

import java.util.List;

/**
 * Physical representation of S1, the unit sphere in 2 dimensions , all points of distance 1 from the origin.
 */
public class UnitSphere extends Sphere1 {
    public UnitSphere(List<MyCylinder> cylinders) {
        super(cylinders);
    }

    @Override
    protected float helixX(int count, float indexFloat, float s) {
        return (float) Math.cos(2 * Math.PI * (indexFloat / count));
    }

    @Override
    protected float helixY(int count, float indexFloat) {
        return 0F;
    }

    @Override
    protected float helixZ(int count, float indexFloat, float s) {
        return (float) Math.sin(2 * Math.PI * (indexFloat / count));
    }

    @Override
    protected float calculateSlant(int cylinderCount, float rads) {
        return 0F;
    }

    @Override
    protected float upwardTranslation() {
        return 0F;
    }

    @Override
    protected CylinderPosition calculatePosition(int count, int index, float rads) {
        float y = 0;
        // TODO this "works" but it makes no sense. Please refactor "y + rads".
        float x = helixX(count, index, y + rads);
        float z = helixZ(count, index, y + rads);

        final float horizontalRotation = (float) (Math.atan(x / z) + Math.PI / 2);

        return new CylinderPosition(x, y, z, horizontalRotation);
    }
}
