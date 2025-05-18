package net.sr89.topology.shapes;

import java.util.List;

/**
 * Physical representation of S1, the unit sphere in 2 dimensions, all points of distance 1 from the origin.
 */
public class UnitSphere extends Sphere1 {
    public UnitSphere(List<MyCylinder> cylinders) {
        super(cylinders);
    }

    @Override
    protected float helixX(int count, float index, float rads) {
        // for the unit sphere, we are calculating the x and z coordinates based on
        // where this cylinder is compared to the total cylinder count,
        // because we want to make one circle (0-th cylinder is the same as count-th cylinder)
        return (float) Math.cos(2 * Math.PI * (index / count));
    }

    @Override
    protected float helixY(int count, float index) {
        return 0F;
    }

    @Override
    protected float helixZ(int count, float index, float rads) {
        return (float) Math.sin(2 * Math.PI * (index / count));
    }

    @Override
    protected float calculateSlant(int cylinderCount, float rads) {
        return 0F;
    }

    @Override
    protected float upwardTranslation() {
        return 0F;
    }
}
