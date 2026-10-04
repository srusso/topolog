package net.sr89.topology.input;

public class CameraMovementService {
    private float forward = 0; // forward == 1, backward == -1
    private float leftRight = 0; // left == -1, right == 1
    private float upDown = 0; // up == 1, down == -1
    private static final float DEGREES_PER_PIXEL = 0.2f;
    private static final float MOVEMENT_SPEED = 20f;

    private boolean hasPreviousMousePosition = false;
    private int previousScreenX;
    private int previousScreenY;
    private float yawPixels = 0;
    private float pitchPixels = 0;

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
        return directionMovement * (MOVEMENT_SPEED * deltaTime);
    }

    /** Accumulates how far the mouse moved (in pixels) since the last call to {@link #resetRotations()}. */
    public void mouseMoved(int screenX, int screenY) {
        if (hasPreviousMousePosition) {
            yawPixels += previousScreenX - screenX;
            pitchPixels += previousScreenY - screenY;
        }
        previousScreenX = screenX;
        previousScreenY = screenY;
        hasPreviousMousePosition = true;
    }

    /** Degrees to turn left/right. Mouse deltas are already per-frame distances, so no deltaTime scaling. */
    public float horizontalRotation() {
        return yawPixels * DEGREES_PER_PIXEL;
    }

    /** Degrees to turn up/down. */
    public float verticalRotation() {
        return pitchPixels * DEGREES_PER_PIXEL;
    }

    public void resetRotations() {
        yawPixels = 0;
        pitchPixels = 0;
    }
}
