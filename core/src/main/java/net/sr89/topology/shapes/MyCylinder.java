package net.sr89.topology.shapes;

import com.badlogic.gdx.graphics.g3d.ModelInstance;

import java.util.UUID;

public class MyCylinder {
    private final String id = UUID.randomUUID().toString();
    private final ModelInstance cylinder;
    private final float s;
    private final float prevX;
    private final float prevY;
    private final float prevZ;
    private final float newX;
    private final float newY;
    private final float newZ;

    public MyCylinder(ModelInstance cylinder, float s, float prevX, float prevY, float prevZ, float newX, float newY, float newZ) {
        this.cylinder = cylinder;
        this.s = s;
        this.prevX = prevX;
        this.prevY = prevY;
        this.prevZ = prevZ;
        this.newX = newX;
        this.newY = newY;
        this.newZ = newZ;
    }

    public String getId() {
        return id;
    }

    public ModelInstance getCylinder() {
        return cylinder;
    }

    public float getS() {
        return s;
    }

    public float getPrevX() {
        return prevX;
    }

    public float getPrevY() {
        return prevY;
    }

    public float getPrevZ() {
        return prevZ;
    }

    public float getNewX() {
        return newX;
    }

    public float getNewY() {
        return newY;
    }

    public float getNewZ() {
        return newZ;
    }
}
