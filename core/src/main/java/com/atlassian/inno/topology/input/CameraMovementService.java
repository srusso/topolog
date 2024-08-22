package com.atlassian.inno.topology.input;

public class CameraMovementService {
    private int forward = 0; // forward == 1, backward == -1
    private int leftRight = 0; // left == -1, right == 1
    private int upDown = 0; // up == 1, down == -1

    public void beginMovementForward() {
        forward = 1;
    }

    public void beginMovementBackward() {
        forward = -1;
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
}
