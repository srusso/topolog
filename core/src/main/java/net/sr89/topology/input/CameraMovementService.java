package net.sr89.topology.input;

import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.Vector3;

public class CameraMovementService {
    private float forward = 0; // forward == 1, backward == -1
    private float leftRight = 0; // left == -1, right == 1
    private float upDown = 0; // up == 1, down == -1
    private float previousScreenX;
    private float currentScreenX;
    private float previousScreenY;
    private float currentScreenY;
    private Vector3 lookAt = new Vector3(0, (float) - Math.sin(2 * Math.PI), (float) Math.cos(2 * Math.PI)).nor();

    public void beginMovementForward() {
        forward = -1;
    }

    public void beginMovementBackward() {
        forward = 1;
    }

    public void beginMovementUp() {
        upDown = 1;
    }

    public void beginMovementDown() {
        upDown = -1;
    }

    public void beginMovementLeft() {
        leftRight = -1;
    }

    public void beginMovementRight() {
        leftRight = 1;
    }

    public void stopMovementForwardBackward() {
        forward = 0;
    }

    public void stopMovementLeftRight() {
        leftRight = 0;
    }

    public void stopMovementUpDown() {
        upDown = 0;
    }

    public Vector3 cameraMovement(float deltaTime) {
        return new Vector3(movementDelta(leftRight, deltaTime), movementDelta(upDown, deltaTime), movementDelta(forward, deltaTime));
    }

    private float movementDelta(float directionMovement, float deltaTime) {
        return directionMovement * (20f * deltaTime);
    }

    public void mouseMoved(int screenX, int screenY) {
        this.currentScreenX = screenX;
        this.currentScreenY = screenY;
    }

    public Vector3 getLookAt(Vector3 cameraPosition, float deltaTime) {
//        this.lookAt = new Vector3(helixZ(screenX + (20f * deltaTime)), 0, helixX(screenX + (20f * deltaTime)));
        Vector3 pos = new Vector3(cameraPosition);
        Vector3 la = new Vector3(lookAt);

        return pos.add(la);
    }

    private static float helixZ(float s) {
        return (float) Math.sin(2 * Math.PI * s);
    }

    private static float helixY(float s) {
        return s;
    }

    private static float helixX(float s) {
        return (float) Math.cos(2 * Math.PI * s);
    }

    public float horizontalRotation(float deltaTime) {
        float result = (currentScreenX - previousScreenX) * 0.00002f;
        previousScreenX = currentScreenX;
        previousScreenY = currentScreenY;
        return result;
    }
}
