package net.sr89.topology.input;

public class CameraMovementService {
    private float forward = 0; // forward == 1, backward == -1
    private float leftRight = 0; // left == -1, right == 1
    private float upDown = 0; // up == 1, down == -1
    private float previousScreenX;
    private float currentScreenX;
    private float previousScreenY;
    private float currentScreenY;

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

    public float upDownMovementDelta(float deltaTime) {
        return movementDelta(upDown, deltaTime);
    }

    public float forwardMovementDelta(float deltaTime) {
        return -movementDelta(forward, deltaTime);
    }

    public float leftRightMovementDelta(float deltaTime) {
        return movementDelta(leftRight, deltaTime);
    }

    private float movementDelta(float directionMovement, float deltaTime) {
        return directionMovement * (20f * deltaTime);
    }

    public void mouseMoved(int screenX, int screenY) {
        if (previousScreenY == 0) {
            this.previousScreenX = screenX;
            this.previousScreenY = screenY;
        }
        this.currentScreenX = screenX;
        this.currentScreenY = screenY;
    }

    public float horizontalRotation(float deltaTime) {
        return (previousScreenX - currentScreenX) * 0.2f;
    }

    public float verticalRotation(float deltaTime) {
        return (previousScreenY - currentScreenY) * 0.2f;
    }

    public void resetRotations() {
        previousScreenX = currentScreenX;
        previousScreenY = currentScreenY;
    }
}
