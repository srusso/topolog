package net.sr89.topology.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.math.MathUtils;

/**
 * Turns the keyboard and mouse state into camera movement. Everything is polled each frame, so
 * there's no key state to get out of sync (e.g. with two opposite keys held, or a missed key release).
 */
public class CameraMovementService {
    private static final float DEGREES_PER_PIXEL = 0.2f;
    private static final float MOVEMENT_SPEED = 20f;

    private boolean seenFirstMouseMove = false;
    private int yawPixels = 0;
    private int pitchPixels = 0;

    /** The cursor is captured, so the mouse can turn the camera indefinitely without hitting the screen edge. */
    public void captureMouse() {
        Gdx.input.setCursorCatched(true);
    }

    public float forwardMovementDelta(float deltaTime) {
        return movementDelta(axis(Keys.W, Keys.S), deltaTime);
    }

    public float leftRightMovementDelta(float deltaTime) {
        return movementDelta(axis(Keys.D, Keys.A) + axis(Keys.RIGHT, Keys.LEFT), deltaTime);
    }

    public float upDownMovementDelta(float deltaTime) {
        return movementDelta(axis(Keys.UP, Keys.DOWN), deltaTime);
    }

    /** Reads the mouse movement for this frame. Call once per frame, before reading the rotations. */
    public void update() {
        final int dx = Gdx.input.getDeltaX();
        final int dy = Gdx.input.getDeltaY();
        if (!seenFirstMouseMove && (dx != 0 || dy != 0)) {
            // The very first mouse event is measured from an initial position of (0, 0), not from where the
            // cursor actually was, so it's a huge bogus jump. Ignore it.
            seenFirstMouseMove = true;
            yawPixels = 0;
            pitchPixels = 0;
            return;
        }
        yawPixels = dx;
        pitchPixels = dy;
    }

    /** Degrees to turn left/right. Mouse deltas are already per-frame distances, so no deltaTime scaling. */
    public float horizontalRotation() {
        return -yawPixels * DEGREES_PER_PIXEL;
    }

    /** Degrees to turn up/down. */
    public float verticalRotation() {
        return -pitchPixels * DEGREES_PER_PIXEL;
    }

    /** 1 if only the positive key is held, -1 if only the negative key is held, otherwise 0. */
    private static float axis(int positiveKey, int negativeKey) {
        return (Gdx.input.isKeyPressed(positiveKey) ? 1 : 0) - (Gdx.input.isKeyPressed(negativeKey) ? 1 : 0);
    }

    private static float movementDelta(float direction, float deltaTime) {
        return MathUtils.clamp(direction, -1f, 1f) * MOVEMENT_SPEED * deltaTime;
    }
}
