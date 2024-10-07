package net.sr89.topology.input;

import com.badlogic.gdx.math.Vector3;

public class CameraMovementService {
    private float forward = 0; // forward == 1, backward == -1
    private float leftRight = 0; // left == -1, right == 1
    private float upDown = 0; // up == 1, down == -1

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
}
